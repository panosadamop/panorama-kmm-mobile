package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun imageCacheDir(context: PlatformContext): Path {
    val cachesDirectory = NSSearchPathForDirectoriesInDomains(
        directory = NSCachesDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    ).first() as String
    return "$cachesDirectory/image_cache".toPath()
}