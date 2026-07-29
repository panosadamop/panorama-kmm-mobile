package gr.panoramapolihnitou.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.LocalPlatformContext
import androidx.compose.ui.unit.Density
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import gr.panoramapolihnitou.app.data.local.BookmarkImageCache
import gr.panoramapolihnitou.app.data.local.PreferencesStore
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.SplashScreen
import gr.panoramapolihnitou.app.ui.navigation.AppDrawer
import gr.panoramapolihnitou.app.ui.navigation.DrawerController
import gr.panoramapolihnitou.app.ui.navigation.LocalDrawerController
import gr.panoramapolihnitou.app.ui.screens.home.HomeScreen
import gr.panoramapolihnitou.app.ui.theme.FontScaleController
import gr.panoramapolihnitou.app.ui.theme.LocalFontScaleController
import gr.panoramapolihnitou.app.ui.theme.PanoramaTheme
import gr.panoramapolihnitou.app.util.LocalSharer
import gr.panoramapolihnitou.app.util.NoopSharer
import gr.panoramapolihnitou.app.util.Sharer
import kotlinx.coroutines.launch

/**
 * Root composable, shared by Android (`MainActivity`) and iOS (`MainViewController`).
 * Shows the animated splash, then hosts the Voyager navigator inside a side
 * navigation drawer (no bottom tabs — matches the reference app). Also owns the
 * app-wide font-scale override so text can be enlarged on every screen.
 */
@Composable
fun App(sharer: Sharer = NoopSharer) {
    PanoramaTheme {
        CompositionLocalProvider(LocalSharer provides sharer) {
            val platformContext = LocalPlatformContext.current
            LaunchedEffect(Unit) {
                AppGraph.bookmarkStore.bookmarks.collect { bookmarks ->
                    BookmarkImageCache.sync(platformContext, bookmarks)
                }
            }

            val prefs = AppGraph.preferences
            val fontScale by prefs.fontScale.collectAsState()

            val controller = FontScaleController(
                scale = fontScale,
                canIncrease = fontScale < PreferencesStore.MAX_SCALE,
                canDecrease = fontScale > PreferencesStore.MIN_SCALE,
                increase = { prefs.increase() },
                decrease = { prefs.decrease() }
            )

            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalFontScaleController provides controller,
                LocalDensity provides Density(
                    density = baseDensity.density,
                    fontScale = baseDensity.fontScale * fontScale
                )
            ) {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinish = { showSplash = false })
                } else {
                    RootNavigation()
                }
            }
        }
    }
}

@Composable
private fun RootNavigation() {
    Navigator(HomeScreen()) { navigator ->
        val drawerState = rememberDrawerState(DrawerValue.Closed)
        val scope = rememberCoroutineScope()
        val controller = remember(drawerState) {
            DrawerController(
                open = { scope.launch { drawerState.open() } },
                close = { scope.launch { drawerState.close() } }
            )
        }
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                AppDrawer(
                    navigator = navigator,
                    onClose = { scope.launch { drawerState.close() } }
                )
            }
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                CompositionLocalProvider(LocalDrawerController provides controller) {
                    SlideTransition(navigator)
                }
            }
        }
    }
}
