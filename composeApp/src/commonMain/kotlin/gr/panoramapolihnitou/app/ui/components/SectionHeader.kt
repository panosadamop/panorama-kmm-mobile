package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import gr.panoramapolihnitou.app.util.removeGreekAccents

/** Section title with a red accent bar, matching the reference SectionHeader. */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(width = 4.dp, height = 20.dp).background(PanoramaColors.primary))
        Text(
            text = removeGreekAccents(title).uppercase(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = PanoramaColors.text,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}
