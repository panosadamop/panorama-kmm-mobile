package gr.panoramapolihnitou.app.data.local

import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small persisted preferences. Currently holds the user's font-scale choice,
 * applied app-wide so text on every page/post/list can be enlarged.
 */
class PreferencesStore(
    private val settings: Settings = Settings()
) {
    private val fontScaleKey = "font_scale_v1"

    private val _fontScale = MutableStateFlow(
        settings.getFloat(fontScaleKey, DEFAULT_SCALE).coerceIn(MIN_SCALE, MAX_SCALE)
    )
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    fun setFontScale(value: Float) {
        val clamped = value.coerceIn(MIN_SCALE, MAX_SCALE)
        settings.putFloat(fontScaleKey, clamped)
        _fontScale.value = clamped
    }

    fun increase() = setFontScale(roundStep(_fontScale.value + STEP))
    fun decrease() = setFontScale(roundStep(_fontScale.value - STEP))

    private fun roundStep(v: Float): Float = (kotlin.math.round(v / STEP) * STEP)

    companion object {
        const val MIN_SCALE = 0.9f
        const val MAX_SCALE = 1.8f
        // Baseline is a little larger than 1.0 for comfortable reading. This only
        // applies when the user hasn't chosen a size; a value they set with the
        // A−/A+ control is persisted and always takes precedence over this default.
        const val DEFAULT_SCALE = 1.05f
        const val STEP = 0.1f
    }
}
