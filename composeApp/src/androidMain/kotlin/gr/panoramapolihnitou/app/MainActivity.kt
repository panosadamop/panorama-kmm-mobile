package gr.panoramapolihnitou.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import gr.panoramapolihnitou.app.notifications.EXTRA_POST_ID
import gr.panoramapolihnitou.app.ui.navigation.DeepLinks
import gr.panoramapolihnitou.app.util.AndroidSharer

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op either way */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermissionIfNeeded()
        if (savedInstanceState == null) handleNotificationTap(intent)
        setContent {
            App(sharer = AndroidSharer(this))
        }
    }

    // launchMode="singleTop": a tap while the app is open arrives here.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNotificationTap(intent)
    }

    /**
     * Notification taps carry the WordPress post id — either as our own extra
     * (foreground, see PanoramaMessagingService) or as the FCM data payload that
     * the system copies into the launch intent (background).
     */
    private fun handleNotificationTap(intent: Intent?) {
        DeepLinks.openArticle(intent?.getStringExtra(EXTRA_POST_ID))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
