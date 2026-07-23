package gr.panoramapolihnitou.app.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide font-size control. The current [scale] is applied by overriding
 * LocalDensity's fontScale at the root, so every `.sp` text on every screen
 * (home, lists, posts, static pages) grows/shrinks together.
 */
class FontScaleController(
    val scale: Float,
    val canIncrease: Boolean,
    val canDecrease: Boolean,
    val increase: () -> Unit,
    val decrease: () -> Unit
)

val LocalFontScaleController = staticCompositionLocalOf<FontScaleController> {
    error("No FontScaleController provided")
}
