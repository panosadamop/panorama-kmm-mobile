package gr.panoramapolihnitou.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Light-only Material 3 scheme built from [PanoramaColors]. The reference app is
 * light-only (`userInterfaceStyle: "light"`), so no dark scheme is provided.
 */
private val LightColors = lightColorScheme(
    primary = PanoramaColors.primary,
    onPrimary = PanoramaColors.textWhite,
    primaryContainer = PanoramaColors.primaryDark,
    onPrimaryContainer = PanoramaColors.textWhite,
    secondary = PanoramaColors.secondary,
    onSecondary = PanoramaColors.textWhite,
    tertiary = PanoramaColors.accent,
    onTertiary = PanoramaColors.text,
    background = Color.White,
    onBackground = PanoramaColors.text,
    surface = PanoramaColors.surface,
    onSurface = PanoramaColors.text,
    surfaceVariant = Color(0xFFEDEDED),
    onSurfaceVariant = PanoramaColors.textSecondary,
    outline = PanoramaColors.border,
    error = PanoramaColors.breaking,
    onError = PanoramaColors.textWhite
)

@Composable
fun PanoramaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = PanoramaTypography(),
        content = content
    )
}
