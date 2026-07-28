package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun imageCacheDir(context: PlatformContext): Path =
    context.cacheDir.resolve("image_cache").toOkioPath()