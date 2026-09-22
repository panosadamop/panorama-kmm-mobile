package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

actual fun bookmarkImageCacheDir(context: PlatformContext): Path {
    val supportDirectory = NSSearchPathForDirectoriesInDomains(
        directory = NSApplicationSupportDirectory,
        domainMask = NSUserDomainMask,
        expandTilde = true
    ).first() as String
    return "$supportDirectory/bookmark_images".toPath()
}
