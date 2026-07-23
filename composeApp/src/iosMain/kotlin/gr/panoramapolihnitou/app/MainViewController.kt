package gr.panoramapolihnitou.app

import androidx.compose.ui.window.ComposeUIViewController
import gr.panoramapolihnitou.app.util.IosSharer
import platform.UIKit.UIViewController

/** Entry point consumed by the SwiftUI `ComposeView` in iosApp. */
fun MainViewController(): UIViewController = ComposeUIViewController {
    App(sharer = IosSharer())
}
