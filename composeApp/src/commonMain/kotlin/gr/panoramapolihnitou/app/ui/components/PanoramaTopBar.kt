package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.resources.Res
import gr.panoramapolihnitou.app.resources.logo
import gr.panoramapolihnitou.app.ui.navigation.LocalDrawerController
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import org.jetbrains.compose.resources.painterResource

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
            // Always show the centered logo (never a page/post title).
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(34.dp).aspectRatio(541f / 168f)
                )
            }
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
