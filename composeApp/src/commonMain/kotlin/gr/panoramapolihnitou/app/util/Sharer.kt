package gr.panoramapolihnitou.app.util

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Platform hook for native "share" sheets (social media sharing per the spec).
 * Android → ACTION_SEND chooser; iOS → UIActivityViewController.
 * Provided at the root of the composition via [LocalSharer].
 */
interface Sharer {
    fun shareText(title: String, text: String, url: String)
}

/** No-op default so previews and tests don't crash. */
object NoopSharer : Sharer {
    override fun shareText(title: String, text: String, url: String) = Unit
}

val LocalSharer = staticCompositionLocalOf<Sharer> { NoopSharer }
