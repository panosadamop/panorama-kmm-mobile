package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.panoramapolihnitou.app.ui.navigation.LocalDrawerController
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import gr.panoramapolihnitou.app.util.removeGreekAccents

/**
 * Red brand header matching the reference app: an optional back button, the
 * hamburger (menu) button — present on every page — a centered ALL-CAPS Greek
 * title (accents dropped, per Greek uppercase convention), then trailing
 * actions, an optional search button, and the always-present font-size control.
 */
@Composable
fun PanoramaTopBar(
    title: String,
    modifier: Modifier = Modifier,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    showSearch: Boolean = false,
    onSearch: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val drawer = LocalDrawerController.current
    Surface(color = PanoramaColors.primary, shadowElevation = 4.dp, modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showBack && onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Πίσω",
                        tint = PanoramaColors.textWhite
                    )
                }
            }
            // Hamburger — shown on every page.
            IconButton(onClick = drawer.open) {
                Icon(
                    Icons.Filled.Menu,
                    contentDescription = "Μενού",
                    tint = PanoramaColors.textWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = removeGreekAccents(title).uppercase(),
                color = PanoramaColors.textWhite,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 16.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            )
            actions()
            if (showSearch && onSearch != null) {
                IconButton(onClick = onSearch) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Αναζήτηση",
                        tint = PanoramaColors.textWhite
                    )
                }
            }
            FontSizeControl()
        }
    }
}
