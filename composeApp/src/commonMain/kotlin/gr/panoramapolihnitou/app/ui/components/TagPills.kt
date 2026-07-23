package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors

/**
 * Renders post tags as rounded pills. Used consistently across the app
 * (cards, article detail, …). [max] optionally caps how many are shown.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagPills(
    tags: List<String>,
    modifier: Modifier = Modifier,
    max: Int? = null
) {
    if (tags.isEmpty()) return
    val shown = if (max != null) tags.take(max) else tags
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        shown.forEach { tag -> TagPill(tag) }
    }
}

@Composable
fun TagPill(tag: String, modifier: Modifier = Modifier) {
    Text(
        text = tag,
        style = MaterialTheme.typography.labelMedium,
        color = PanoramaColors.primary,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(PanoramaColors.primary.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}
