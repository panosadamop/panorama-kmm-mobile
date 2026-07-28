package gr.panoramapolihnitou.app.util

import coil3.PlatformContext
import okio.Path

/** Platform cache directory to store Coil's on-disk image cache. */
expect fun imageCacheDir(context: PlatformContext): Path