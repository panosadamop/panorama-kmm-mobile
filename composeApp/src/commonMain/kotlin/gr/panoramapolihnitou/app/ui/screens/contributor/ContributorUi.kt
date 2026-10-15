package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.data.remote.ContributorException
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

/** Red hero band, same look as the Contact screen's header. */
@Composable
internal fun ContributorHero(icon: ImageVector, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().background(PanoramaColors.primary).padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = PanoramaColors.textWhite, modifier = Modifier.size(44.dp))
        Text(
            title,
            color = PanoramaColors.textWhite,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            subtitle,
            color = PanoramaColors.textWhite,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
internal fun ContributorSectionTitle(title: String) {
    Row(
        Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 4.dp, height = 18.dp).background(PanoramaColors.primary))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PanoramaColors.text,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
internal fun ContributorNotice(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = PanoramaColors.text,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(PanoramaColors.accent.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
            .padding(14.dp)
    )
}

internal fun Throwable.contributorMessage(): String = when (this) {
    is ContributorException -> message ?: "Κάτι πήγε στραβά. Δοκιμάστε ξανά."
    is IllegalStateException -> message ?: "Μη αναμενόμενη απάντηση από τον διακομιστή."
    else -> "Δεν ήταν δυνατή η σύνδεση. Ελέγξτε τη σύνδεσή σας και δοκιμάστε ξανά."
}

/** 3_355_443 → "3,2 MB" (Greek decimal comma). */
internal fun formatMegabytes(bytes: Long): String {
    val tenths = bytes * 10 / (1024 * 1024)
    return "${tenths / 10},${tenths % 10} MB"
}

/** 75 → "01:15". */
internal fun formatDuration(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
