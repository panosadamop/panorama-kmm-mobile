package gr.panoramapolihnitou.app

import android.app.Application
import com.google.android.gms.ads.MobileAds
import com.google.firebase.messaging.FirebaseMessaging
import gr.panoramapolihnitou.app.notifications.NEW_ARTICLES_TOPIC

/**
 * Application entry point. Initialises the Google Mobile Ads SDK for banner ads
 * and subscribes to the push-notification topic for new articles.
 * (multiplatform-settings resolves the application context automatically, so no
 * other DI initialisation is required.)
 */
class PanoramaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Safe to call on the main thread; the SDK does its work in the background.
        runCatching { MobileAds.initialize(this) }
        // No-ops safely if google-services.json hasn't been configured yet
        // (Firebase then has no default app to subscribe with).
        runCatching { FirebaseMessaging.getInstance().subscribeToTopic(NEW_ARTICLES_TOPIC) }
    }
}
