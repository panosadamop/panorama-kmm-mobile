package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.data.remote.ContributorException
import gr.panoramapolihnitou.app.data.remote.ContributorResponse
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.media.LiveBroadcaster
import gr.panoramapolihnitou.app.media.LiveCameraPreview
import gr.panoramapolihnitou.app.media.LiveStatus
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Full-screen camera that broadcasts to the channel's YouTube stream key.
 * The key is fetched from the backend each time (codes without the `live`
 * role are refused there), so it's never stored on the device.
 */
class LiveBroadcastScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val session = AppGraph.contributorSession

        var config by remember { mutableStateOf<ContributorResponse?>(null) }
        var message by remember { mutableStateOf<String?>(null) }
        var broadcaster by remember { mutableStateOf<LiveBroadcaster?>(null) }
        var confirmExit by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            val code = session.current.value?.code
            if (code == null) {
                navigator.pop()
                return@LaunchedEffect
            }
            runCatching { AppGraph.contributorApi.liveConfig(code) }
                .onSuccess { config = it }
                .onFailure {
                    message = it.contributorMessage()
                    if (it is ContributorException && it.isInvalidCode) session.clear()
                }
        }

        val status by remember(broadcaster) {
            broadcaster?.status ?: MutableStateFlow(LiveStatus.Idle)
        }.collectAsState()
        val onAir = status == LiveStatus.Live || status == LiveStatus.Connecting

        fun leave() {
            if (onAir) confirmExit = true else navigator.pop()
        }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            LiveCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onReady = { ready ->
                    broadcaster = ready
                    if (ready == null) message = "Η ζωντανή μετάδοση δεν είναι διαθέσιμη σε αυτή τη συσκευή."
                },
                onError = { message = it }
            )

            // Top overlay: back + status badge
            Row(
                Modifier.fillMaxWidth().windowInsetsPadding(WindowInsets.safeDrawing).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { leave() }, modifier = Modifier.background(Color(0x66000000), CircleShape)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Πίσω", tint = Color.White)
                }
                Spacer(Modifier.weight(1f))
                StatusBadge(status)
            }

            // Bottom overlay: messages + controls
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val failure = (status as? LiveStatus.Failed)?.message ?: message
                failure?.let {
                    Text(
                        it,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            .background(Color(0x99000000), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.size(52.dp))
                    RecordButton(
                        onAir = onAir,
                        connecting = status == LiveStatus.Connecting,
                        enabled = broadcaster != null && config != null
                    ) {
                        val b = broadcaster ?: return@RecordButton
                        val c = config ?: return@RecordButton
                        if (onAir) {
                            b.stop()
                        } else {
                            message = null
                            b.start(c.rtmpUrl.orEmpty(), c.streamKey.orEmpty())
                        }
                    }
                    IconButton(
                        onClick = { broadcaster?.switchCamera() },
                        enabled = broadcaster != null,
                        modifier = Modifier.size(52.dp).background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(Icons.Filled.Cameraswitch, contentDescription = "Αλλαγή κάμερας", tint = Color.White)
                    }
                }
                if (config == null && message == null) {
                    Text(
                        "Προετοιμασία μετάδοσης…",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        }

        if (confirmExit) {
            AlertDialog(
                onDismissRequest = { confirmExit = false },
                title = { Text("Τερματισμός μετάδοσης;") },
                text = { Text("Η ζωντανή μετάδοση θα σταματήσει αν φύγετε από αυτή την οθόνη.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmExit = false
                        broadcaster?.stop()
                        navigator.pop()
                    }) { Text("Τερματισμός", color = PanoramaColors.breaking) }
                },
                dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Συνέχεια") } }
            )
        }
    }
}

@Composable
private fun StatusBadge(status: LiveStatus) {
    val (label, color) = when (status) {
        LiveStatus.Live -> "● ΖΩΝΤΑΝΑ" to PanoramaColors.breaking
        LiveStatus.Connecting -> "ΣΥΝΔΕΣΗ…" to PanoramaColors.accent
        is LiveStatus.Failed -> "ΣΦΑΛΜΑ" to Color(0xFF555555)
        LiveStatus.Idle -> "ΕΚΤΟΣ ΑΕΡΑ" to Color(0x99000000)
    }
    Text(
        label,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelLarge,
        modifier = Modifier.background(color, RoundedCornerShape(6.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun RecordButton(onAir: Boolean, connecting: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(76.dp)
            .border(4.dp, Color.White, CircleShape)
            .padding(8.dp)
            .background(if (enabled) PanoramaColors.breaking else Color(0xFF777777), CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            connecting -> CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            onAir -> Icon(Icons.Filled.Stop, contentDescription = "Διακοπή μετάδοσης", tint = Color.White, modifier = Modifier.size(32.dp))
            else -> Text("LIVE", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
