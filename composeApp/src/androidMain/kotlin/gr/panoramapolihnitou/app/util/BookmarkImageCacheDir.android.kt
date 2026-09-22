package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun bookmarkImageCacheDir(context: PlatformContext): Path =
    context.filesDir.resolve("bookmark_images").toOkioPath()
