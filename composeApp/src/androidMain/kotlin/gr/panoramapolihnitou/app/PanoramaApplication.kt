package gr.panoramapolihnitou.app

import android.app.Application
import com.google.android.gms.ads.MobileAds

/**
 * Application entry point. Initialises the Google Mobile Ads SDK for banner ads.
 * (multiplatform-settings resolves the application context automatically, so no
 * other DI initialisation is required.)
 */
class PanoramaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Safe to call on the main thread; the SDK does its work in the background.
        runCatching { MobileAds.initialize(this) }
    }
}
