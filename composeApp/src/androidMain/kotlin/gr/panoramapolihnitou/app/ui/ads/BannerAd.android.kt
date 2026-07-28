package gr.panoramapolihnitou.app.ui.ads

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * No-op while AdMob is disabled. The 1.0.0 Play release ships without the
 * Google Mobile Ads SDK, because the only ad ids in the project were Google's
 * public *test* ids — shipping those to production violates AdMob policy and
 * shows fake banners to real users.
 *
 * To enable (RUNNING.md → "Banner ads"):
 *  1. Create a real AdMob app + banner ad unit.
 *  2. Add `implementation(libs.play.services.ads)` back to `androidMain`.
 *  3. Restore in `AndroidManifest.xml`: the `com.google.android.gms.permission.AD_ID`
 *     permission and the `com.google.android.gms.ads.APPLICATION_ID` meta-data,
 *     plus an `Application` subclass calling `MobileAds.initialize(this)`.
 *  4. Set `AdConfig.adsEnabled = true` and a real `AdConfig.bannerAdUnitId`.
 *  5. Replace this body with an `AndroidView` hosting an `AdView`
 *     (`setAdSize(AdSize.BANNER)`, `setAdUnitId(adUnitId)`,
 *     `loadAd(AdRequest.Builder().build())`).
 *  6. Re-declare Advertising ID collection in the Play Data safety form.
 */
@Composable
actual fun PlatformBannerAd(modifier: Modifier, adUnitId: String) {
    // Intentionally empty until the Google Mobile Ads SDK is wired back in.
}
