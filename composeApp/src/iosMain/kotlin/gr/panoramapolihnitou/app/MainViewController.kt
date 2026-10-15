package gr.panoramapolihnitou.app

import androidx.compose.ui.window.ComposeUIViewController
import gr.panoramapolihnitou.app.media.IosLiveStreamBridge
import gr.panoramapolihnitou.app.media.IosPlatformBridges
import gr.panoramapolihnitou.app.util.IosSharer
import platform.UIKit.UIViewController

/**
 * Entry point consumed by the SwiftUI `ComposeView` in iosApp. The live-stream
 * bridge is implemented in Swift (HaishinKit); pass nil to build without it.
 */
fun MainViewController(liveStreamBridge: IosLiveStreamBridge?): UIViewController {
    IosPlatformBridges.liveStream = liveStreamBridge
    return ComposeUIViewController {
        App(sharer = IosSharer())
    }
}
