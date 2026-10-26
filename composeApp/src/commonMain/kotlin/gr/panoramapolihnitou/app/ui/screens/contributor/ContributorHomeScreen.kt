package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.data.remote.ContributorException
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

class ContributorHomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val session = AppGraph.contributorSession
        val contributor by session.current.collectAsState()

        val current = contributor
        if (current == null) {
            LaunchedEffect(Unit) { navigator.replace(ContributorLoginScreen()) }
            return
        }

        // Refresh name/roles quietly; a code revoked server-side signs the device out.
        LaunchedEffect(current.code) {
            runCatching { AppGraph.contributorApi.verify(current.code) }
                .onSuccess { session.save(current.copy(name = it.name ?: current.name, roles = it.roles)) }
                .onFailure { if (it is ContributorException && it.endsSession) session.clear() }
        }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Συνεργάτες", showBack = true, onBack = { navigator.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
                ContributorHero(
                    icon = Icons.Outlined.Badge,
                    title = "Καλώς ήρθατε, ${current.name}",
                    subtitle = "Τι θα θέλατε να κάνετε;"
                )
                Spacer(Modifier.height(8.dp))
                if (current.canSubmit) {
                    ActionCard(
                        icon = Icons.Filled.CloudUpload,
                        title = "Αποστολή υλικού",
                        description = "Φωτογραφίες, ηχογραφήσεις και κείμενο προς τη σύνταξη."
                    ) { navigator.push(SubmissionScreen()) }
                }
                if (current.canStream) {
                    ActionCard(
                        icon = Icons.Filled.Videocam,
                        title = "Ζωντανή μετάδοση",
                        description = "Μεταδώστε ζωντανά εικόνα και ήχο στο κανάλι YouTube του Πανοράματος."
                    ) { navigator.push(LiveBroadcastScreen()) }
                }
                if (!current.canSubmit && !current.canStream) {
                    ContributorNotice("Ο κωδικός σας δεν έχει ενεργές δυνατότητες. Επικοινωνήστε με τη σύνταξη.")
                }
                TextButton(
                    onClick = { session.clear() },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PanoramaColors.textSecondary)
                    Text("Αποσύνδεση", color = PanoramaColors.textSecondary, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PanoramaColors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).background(PanoramaColors.primary.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = PanoramaColors.primary, modifier = Modifier.size(26.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = PanoramaColors.text)
                Text(description, style = MaterialTheme.typography.bodyMedium, color = PanoramaColors.textSecondary)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = PanoramaColors.textLight)
        }
    }
}
