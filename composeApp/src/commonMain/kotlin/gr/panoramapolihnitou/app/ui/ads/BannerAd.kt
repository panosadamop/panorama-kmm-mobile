package gr.panoramapolihnitou.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Central switch + ad-unit configuration for banner ads. Defaults use Google's
 * public TEST ad units so the app shows test banners out of the box — replace
 * [bannerAdUnitId] (and the AdMob App ID in the Android manifest / iOS Info.plist)
 * with your real ids before publishing. See RUNNING.md → "Banner ads".
 */
object AdConfig {
    /** Master on/off switch for all banner slots. */
    var adsEnabled: Boolean = true

    /** AdMob banner ad-unit id (test unit by default). */
    var bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111"
}

/**
 * Drop-in banner ad slot — place it anywhere in the UI:
 * `BannerAd(Modifier.padding(vertical = 8.dp))`.
 * Renders a platform banner (AdMob) when ads are enabled; otherwise nothing.
 */
@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.bannerAdUnitId
) {
    if (!AdConfig.adsEnabled || adUnitId.isBlank()) return
    PlatformBannerAd(modifier, adUnitId)
}

/** Platform banner implementation (AdMob on Android; no-op stub on iOS for now). */
@Composable
expect fun PlatformBannerAd(modifier: Modifier, adUnitId: String)
