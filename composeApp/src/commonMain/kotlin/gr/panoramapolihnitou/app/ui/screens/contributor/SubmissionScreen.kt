package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import gr.panoramapolihnitou.app.data.remote.ContributorConfig
import gr.panoramapolihnitou.app.data.remote.SubmissionDestination
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.media.AttachmentKind
import gr.panoramapolihnitou.app.media.MediaAttachment
import gr.panoramapolihnitou.app.media.rememberAudioRecorder
import gr.panoramapolihnitou.app.media.rememberImagePicker
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import kotlinx.coroutines.delay

/** Recordings auto-stop here so a forgotten recorder can't blow the upload limit. */
private const val MAX_RECORDING_SECONDS = 30 * 60

class SubmissionScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { SubmissionScreenModel(AppGraph.contributorApi, AppGraph.contributorSession) }

        val imagePicker = rememberImagePicker(
            onImages = { model.add(it) },
            onError = { model.error = it }
        )
        val recorder = rememberAudioRecorder(
            onRecorded = { model.add(listOf(it)) },
            onError = { model.error = it }
        )
        var elapsed by remember { mutableIntStateOf(0) }
        LaunchedEffect(recorder.isRecording) {
            elapsed = 0
            while (recorder.isRecording) {
                delay(1_000)
                elapsed++
                if (elapsed >= MAX_RECORDING_SECONDS) recorder.stop()
            }
        }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = "Αποστολή υλικού", showBack = true, onBack = { navigator.pop() })
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {

                ContributorSectionTitle("Κείμενο")
                OutlinedTextField(
                    value = model.title,
                    onValueChange = { model.title = it },
                    label = { Text("Τίτλος (προαιρετικά)") },
                    singleLine = true,
                    enabled = !model.sending,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model.text,
                    onValueChange = { model.text = it },
                    label = { Text("Περιγραφή / ρεπορτάζ") },
                    minLines = 5,
                    enabled = !model.sending,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )

                ContributorSectionTitle("Φωτογραφίες & ήχος")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MediaButton(Icons.Filled.PhotoCamera, "Κάμερα", enabled = !model.sending && !recorder.isRecording) {
                        imagePicker.takePhoto()
                    }
                    MediaButton(Icons.Filled.PhotoLibrary, "Συλλογή", enabled = !model.sending && !recorder.isRecording) {
                        imagePicker.pickFromGallery()
                    }
                    if (recorder.isRecording) {
                        Button(
                            onClick = { recorder.stop() },
                            colors = ButtonDefaults.buttonColors(containerColor = PanoramaColors.breaking),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text(formatDuration(elapsed), modifier = Modifier.padding(start = 4.dp))
                        }
                    } else {
                        MediaButton(Icons.Filled.Mic, "Ηχογράφηση", enabled = !model.sending) { recorder.start() }
                    }
                }

                if (model.attachments.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(model.attachments, key = { it.id }) { item ->
                            AttachmentTile(item, removable = !model.sending) { model.remove(item) }
                        }
                    }
                    Text(
                        "Σύνολο: ${formatMegabytes(model.totalBytes)} από ${formatMegabytes(ContributorConfig.MAX_UPLOAD_BYTES)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (model.overLimit) MaterialTheme.colorScheme.error else PanoramaColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    if (model.overLimit) {
                        Text(
                            "Αφαιρέστε κάποια αρχεία ή στείλτε τα σε δύο αποστολές.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                ContributorSectionTitle("Αποστολή σε")
                DestinationOption(
                    selected = model.destination == SubmissionDestination.EMAIL,
                    title = "Email",
                    subtitle = ContributorConfig.MAIL_TO,
                    enabled = !model.sending
                ) { model.destination = SubmissionDestination.EMAIL }
                DestinationOption(
                    selected = model.destination == SubmissionDestination.DRIVE,
                    title = "Google Drive",
                    subtitle = "Φάκελος υλικού της σύνταξης",
                    enabled = !model.sending
                ) { model.destination = SubmissionDestination.DRIVE }

                model.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                Button(
                    onClick = { model.send() },
                    enabled = model.canSend && !recorder.isRecording,
                    colors = ButtonDefaults.buttonColors(containerColor = PanoramaColors.primary),
                    modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp)
                ) {
                    if (model.sending) {
                        CircularProgressIndicator(color = PanoramaColors.textWhite, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        Text("Αποστολή…", modifier = Modifier.padding(start = 12.dp))
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Αποστολή", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                if (model.sending) {
                    Text(
                        "Μην κλείσετε αυτή την οθόνη μέχρι να ολοκληρωθεί η αποστολή.",
                        style = MaterialTheme.typography.bodySmall,
                        color = PanoramaColors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    )
                }
            }
        }

        model.successMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { model.reset() },
                title = { Text("Ευχαριστούμε!") },
                text = { Text(message) },
                confirmButton = { TextButton(onClick = { model.reset() }) { Text("OK") } }
            )
        }
    }
}

@Composable
private fun RowScope.MediaButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
        modifier = Modifier.weight(1f)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = PanoramaColors.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = PanoramaColors.text, maxLines = 1)
        }
    }
}

@Composable
private fun AttachmentTile(item: MediaAttachment, removable: Boolean, onRemove: () -> Unit) {
    Box(Modifier.size(96.dp)) {
        when (item.kind) {
            AttachmentKind.IMAGE -> AsyncImage(
                model = item.bytes,
                contentDescription = item.fileName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
            AttachmentKind.AUDIO -> Column(
                Modifier.fillMaxSize()
                    .background(PanoramaColors.secondary, RoundedCornerShape(10.dp))
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null, tint = PanoramaColors.textWhite)
                Text(
                    formatDuration(item.durationSeconds ?: 0),
                    color = PanoramaColors.textWhite,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(formatMegabytes(item.bytes.size.toLong()), color = PanoramaColors.textWhite, style = MaterialTheme.typography.labelSmall)
            }
        }
        if (removable) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(PanoramaColors.text.copy(alpha = 0.7f), CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Αφαίρεση", tint = PanoramaColors.textWhite, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun DestinationOption(
    selected: Boolean,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onSelect: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled,
            colors = RadioButtonDefaults.colors(selectedColor = PanoramaColors.primary)
        )
        Column(Modifier.padding(start = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = PanoramaColors.text)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PanoramaColors.textSecondary)
        }
    }
}
