package gr.panoramapolihnitou.app.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lets any screen's top bar open/close the app-wide navigation drawer without
 * threading the DrawerState through every composable.
 */
class DrawerController(
    val open: () -> Unit,
    val close: () -> Unit
)

val LocalDrawerController = staticCompositionLocalOf<DrawerController> {
    error("No DrawerController provided")
}
