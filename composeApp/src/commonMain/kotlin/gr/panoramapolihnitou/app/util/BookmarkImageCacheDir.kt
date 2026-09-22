package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path

/**
 * Persistent directory for bookmarked articles' cached images — unlike
 * [imageCacheDir] (Coil's disk cache), the OS won't purge this under storage
 * pressure, so a bookmark stays readable offline until the user un-bookmarks it.
 */
expect fun bookmarkImageCacheDir(context: PlatformContext): Path
