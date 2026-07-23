package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.ui.theme.LocalFontScaleController
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import kotlin.math.roundToInt

/**
 * A single header button that opens a small A− / percentage / A+ control.
 * Available on every page via [PanoramaTopBar], so the reader can enlarge the
 * text on home, lists, posts and static pages alike.
 */
@Composable
fun FontSizeControl(tint: androidx.compose.ui.graphics.Color = PanoramaColors.textWhite) {
    val controller = LocalFontScaleController.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Filled.FormatSize, contentDescription = "Μέγεθος γραμματοσειράς", tint = tint)
    }

    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(onClick = controller.decrease, enabled = controller.canDecrease) {
                Icon(Icons.Filled.Remove, contentDescription = "Μείωση")
            }
            Text(
                text = "${(controller.scale * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(min = 52.dp)
            )
            IconButton(onClick = controller.increase, enabled = controller.canIncrease) {
                Icon(Icons.Filled.Add, contentDescription = "Αύξηση")
            }
        }
    }
}
