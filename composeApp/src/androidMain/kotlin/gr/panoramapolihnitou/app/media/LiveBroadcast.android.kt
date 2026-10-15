package gr.panoramapolihnitou.app.media

import android.Manifest
import android.content.Context
import android.view.SurfaceHolder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.pedro.common.ConnectChecker
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.library.rtmp.RtmpCamera2
import com.pedro.library.view.OpenGlView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val VIDEO_WIDTH = 1280
private const val VIDEO_HEIGHT = 720
/** YouTube recommends 1.5–4 Mbps for 720p30; the lower end survives 4G uplinks. */
private const val VIDEO_BITRATE = 2_500_000

private val LIVE_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)

@Composable
actual fun LiveCameraPreview(
    modifier: Modifier,
    onReady: (LiveBroadcaster?) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnError by rememberUpdatedState(onError)
    var granted by remember { mutableStateOf(LIVE_PERMISSIONS.all { context.hasPermission(it) }) }

    val permissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        granted = result.values.all { it }
        if (!granted) currentOnError("Η μετάδοση χρειάζεται άδεια για κάμερα και μικρόφωνο.")
    }
    LaunchedEffect(Unit) { if (!granted) permissions.launch(LIVE_PERMISSIONS) }

    if (!granted) {
        Box(modifier.background(Color.Black))
        return
    }

    val broadcaster = remember { AndroidLiveBroadcaster(context.applicationContext) }
    DisposableEffect(broadcaster) {
        currentOnReady(broadcaster)
        onDispose { broadcaster.release() }
    }
    AndroidView(
        factory = { ctx ->
            OpenGlView(ctx).also { view ->
                view.keepScreenOn = true
                broadcaster.attach(view)
            }
        },
        modifier = modifier
    )
}

private class AndroidLiveBroadcaster(private val context: Context) : LiveBroadcaster, ConnectChecker {

    private val _status = MutableStateFlow<LiveStatus>(LiveStatus.Idle)
    override val status: StateFlow<LiveStatus> = _status.asStateFlow()

    private var camera: RtmpCamera2? = null

    fun attach(view: OpenGlView) {
        val rtmpCamera = RtmpCamera2(view, this)
        camera = rtmpCamera
        view.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) = Unit

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (!rtmpCamera.isOnPreview) {
                    runCatching { rtmpCamera.startPreview(CameraHelper.Facing.BACK, VIDEO_WIDTH, VIDEO_HEIGHT) }
                        .onFailure { _status.value = LiveStatus.Failed("Δεν ήταν δυνατό το άνοιγμα της κάμερας.") }
                }
            }

            // Backgrounding the app destroys the surface: end the broadcast cleanly.
            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (rtmpCamera.isStreaming) rtmpCamera.stopStream()
                if (rtmpCamera.isOnPreview) rtmpCamera.stopPreview()
                if (_status.value != LiveStatus.Idle) _status.value = LiveStatus.Idle
            }
        })
    }

    override fun start(ingestUrl: String, streamKey: String) {
        val rtmpCamera = camera ?: return
        if (rtmpCamera.isStreaming) return
        if (ingestUrl.isBlank() || streamKey.isBlank()) {
            _status.value = LiveStatus.Failed("Δεν έχει οριστεί κλειδί μετάδοσης YouTube.")
            return
        }
        // Rotation follows the device orientation, so a phone held upright streams portrait.
        val rotation = CameraHelper.getCameraOrientation(context)
        val prepared = rtmpCamera.prepareAudio() &&
            rtmpCamera.prepareVideo(VIDEO_WIDTH, VIDEO_HEIGHT, 30, VIDEO_BITRATE, rotation)
        if (!prepared) {
            _status.value = LiveStatus.Failed("Η συσκευή δεν υποστηρίζει την απαιτούμενη κωδικοποίηση βίντεο.")
            return
        }
        _status.value = LiveStatus.Connecting
        rtmpCamera.startStream("${ingestUrl.trimEnd('/')}/$streamKey")
    }

    override fun stop() {
        camera?.let { if (it.isStreaming) it.stopStream() }
        _status.value = LiveStatus.Idle
    }

    override fun switchCamera() {
        runCatching { camera?.switchCamera() }
    }

    fun release() {
        camera?.let {
            if (it.isStreaming) it.stopStream()
            if (it.isOnPreview) it.stopPreview()
        }
        camera = null
    }

    // ConnectChecker — called on RootEncoder's worker threads; StateFlow is thread-safe.
    override fun onConnectionStarted(url: String) {
        _status.value = LiveStatus.Connecting
    }

    override fun onConnectionSuccess() {
        _status.value = LiveStatus.Live
    }

    override fun onConnectionFailed(reason: String) {
        camera?.let { if (it.isStreaming) it.stopStream() }
        _status.value = LiveStatus.Failed("Η σύνδεση με το YouTube απέτυχε. Ελέγξτε το δίκτυο και δοκιμάστε ξανά.")
    }

    override fun onDisconnect() {
        if (_status.value is LiveStatus.Failed) return
        _status.value = LiveStatus.Idle
    }

    override fun onAuthError() {
        camera?.let { if (it.isStreaming) it.stopStream() }
        _status.value = LiveStatus.Failed("Το YouTube απέρριψε το κλειδί μετάδοσης.")
    }

    override fun onAuthSuccess() = Unit
}
