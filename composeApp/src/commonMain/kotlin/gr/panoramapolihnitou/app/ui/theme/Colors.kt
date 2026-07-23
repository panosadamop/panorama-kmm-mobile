package gr.panoramapolihnitou.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact palette ported from the reference app's `src/theme/colors.js`.
 * Kept as a plain object (not only a Material scheme) because the side drawer
 * uses several brand-specific colors that don't map onto Material roles.
 */
object PanoramaColors {
    val primary = Color(0xFFB22222)       // Deep red — main brand color
    val primaryDark = Color(0xFF8B0000)   // Darker red
    val primaryLight = Color(0xFFDC143C)  // Lighter red
    val secondary = Color(0xFF1A1A2E)     // Dark navy
    val accent = Color(0xFFF5A623)        // Amber accent
    val background = Color(0xFFF4F4F4)    // Light gray background
    val surface = Color(0xFFFFFFFF)       // White surfaces
    val text = Color(0xFF1A1A1A)          // Primary text
    val textSecondary = Color(0xFF666666) // Secondary text
    val textLight = Color(0xFF999999)     // Light text
    val textWhite = Color(0xFFFFFFFF)     // White text
    val border = Color(0xFFE0E0E0)        // Border color
    val breaking = Color(0xFFFF0000)      // Breaking news red
    val categoryChip = Color(0xFF2C3E50)
    val headerBg = Color(0xFFB22222)
    val drawerBg = Color(0xFF1A1A2E)      // Dark navy drawer
    val drawerText = Color(0xFFFFFFFF)
    val drawerAccent = Color(0xFFB22222)
}
