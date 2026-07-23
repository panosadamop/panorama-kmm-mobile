package gr.panoramapolihnitou.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * iOS banner ads require the Google Mobile Ads SDK (`GADBannerView`) linked into
 * the Xcode project via Swift Package Manager or CocoaPods, plus a
 * `GADApplicationIdentifier` in Info.plist. Until that SDK is added this is a
 * no-op so the shared code still compiles and runs.
 *
 * To enable: add the SDK, then replace the body with a `UIKitView` hosting a
 * `GADBannerView` (adUnitID = adUnitId, rootViewController = top VC, load a
 * GADRequest). See RUNNING.md → "Banner ads (iOS)".
 */
@Composable
actual fun PlatformBannerAd(modifier: Modifier, adUnitId: String) {
    // Intentionally empty until the Google Mobile Ads iOS SDK is wired in.
}
