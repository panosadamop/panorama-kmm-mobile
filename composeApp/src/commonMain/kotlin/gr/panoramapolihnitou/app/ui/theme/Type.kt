package gr.panoramapolihnitou.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import gr.panoramapolihnitou.app.resources.Res
import gr.panoramapolihnitou.app.resources.ubuntu_bold
import gr.panoramapolihnitou.app.resources.ubuntu_light
import gr.panoramapolihnitou.app.resources.ubuntu_medium
import gr.panoramapolihnitou.app.resources.ubuntu_regular
import org.jetbrains.compose.resources.Font

/** The Ubuntu font family (bundled TTFs, full Greek coverage). */
@Composable
fun ubuntuFamily(): FontFamily = FontFamily(
    Font(Res.font.ubuntu_light, FontWeight.Light),
    Font(Res.font.ubuntu_regular, FontWeight.Normal),
    Font(Res.font.ubuntu_medium, FontWeight.Medium),
    Font(Res.font.ubuntu_medium, FontWeight.SemiBold),
    Font(Res.font.ubuntu_bold, FontWeight.Bold)
)

/** Material 3 type scale rendered entirely in Ubuntu. */
@Composable
fun PanoramaTypography(): Typography {
    val ubuntu = ubuntuFamily()
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = ubuntu),
        displayMedium = d.displayMedium.copy(fontFamily = ubuntu),
        displaySmall = d.displaySmall.copy(fontFamily = ubuntu),
        headlineLarge = d.headlineLarge.copy(fontFamily = ubuntu, fontWeight = FontWeight.Bold),
        headlineMedium = d.headlineMedium.copy(fontFamily = ubuntu, fontWeight = FontWeight.Bold),
        headlineSmall = d.headlineSmall.copy(fontFamily = ubuntu, fontWeight = FontWeight.Bold),
        titleLarge = d.titleLarge.copy(fontFamily = ubuntu, fontWeight = FontWeight.Bold),
        titleMedium = d.titleMedium.copy(fontFamily = ubuntu, fontWeight = FontWeight.SemiBold),
        titleSmall = d.titleSmall.copy(fontFamily = ubuntu, fontWeight = FontWeight.Medium),
        bodyLarge = d.bodyLarge.copy(fontFamily = ubuntu),
        bodyMedium = d.bodyMedium.copy(fontFamily = ubuntu),
        bodySmall = d.bodySmall.copy(fontFamily = ubuntu),
        labelLarge = d.labelLarge.copy(fontFamily = ubuntu, fontWeight = FontWeight.Medium),
        labelMedium = d.labelMedium.copy(fontFamily = ubuntu, fontWeight = FontWeight.Medium),
        labelSmall = d.labelSmall.copy(fontFamily = ubuntu)
    )
}
