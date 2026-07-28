package gr.panoramapolihnitou.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Central switch + ad-unit configuration for banner ads.
 *
 * Ads are **off** for the store releases: the project only ever had Google's
 * public TEST ad ids, which must not ship to production. The Ads SDK is not
 * linked on either platform, so every [BannerAd] slot renders nothing.
 *
 * To go live, set [bannerAdUnitId] to a real `ca-app-pub-…/…` unit, flip
 * [adsEnabled], and follow the platform steps in RUNNING.md → "Banner ads".
 */
object AdConfig {
    /** Master on/off switch for all banner slots. */
    var adsEnabled: Boolean = false

    /** AdMob banner ad-unit id — set a real one before enabling ads. */
    var bannerAdUnitId: String = ""
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
