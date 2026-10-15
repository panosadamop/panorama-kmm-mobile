package gr.panoramapolihnitou.app.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.UIKit.UIView

/**
 * Status callback from Swift. `status` is one of `idle`, `connecting`, `live`,
 * `error`; for `error`, `reason` is `permission`, `connect`, `closed` or `rejected`.
 */
interface IosLiveStreamListener {
    fun onStatus(status: String, reason: String?)
}

/**
 * RTMP publishing on iOS is done by HaishinKit, a Swift-only library Kotlin
 * can't call. iosApp implements this interface in `LiveStreamBridge.swift`
 * and passes it to `MainViewController(liveStreamBridge:)`.
 */
interface IosLiveStreamBridge {
    fun makePreviewView(): UIView

    /** Asks for camera + microphone access, then starts the capture session. */
    fun attachCamera()
    fun startStream(url: String, streamKey: String)
    fun stopStream()
    fun switchCamera()

    /** Stops everything and releases the camera; called when the screen goes away. */
    fun detach()
    fun bindListener(listener: IosLiveStreamListener?)
}

internal object IosPlatformBridges {
    var liveStream: IosLiveStreamBridge? = null
}

@Composable
actual fun LiveCameraPreview(
    modifier: Modifier,
    onReady: (LiveBroadcaster?) -> Unit,
    onError: (String) -> Unit
) {
    val currentOnReady by rememberUpdatedState(onReady)
    val bridge = IosPlatformBridges.liveStream
    if (bridge == null) {
        LaunchedEffect(Unit) { currentOnReady(null) }
        Box(modifier.background(Color.Black))
        return
    }

    val broadcaster = remember(bridge) { IosLiveBroadcaster(bridge) }
    DisposableEffect(broadcaster) {
        broadcaster.open()
        currentOnReady(broadcaster)
        onDispose { broadcaster.close() }
    }
    UIKitView(factory = { bridge.makePreviewView() }, modifier = modifier)
}

private class IosLiveBroadcaster(private val bridge: IosLiveStreamBridge) : LiveBroadcaster {

    private val _status = MutableStateFlow<LiveStatus>(LiveStatus.Idle)
    override val status: StateFlow<LiveStatus> = _status.asStateFlow()

    private val listener = object : IosLiveStreamListener {
        override fun onStatus(status: String, reason: String?) {
            _status.value = when (status) {
                "connecting" -> LiveStatus.Connecting
                "live" -> LiveStatus.Live
                "error" -> LiveStatus.Failed(failureMessage(reason))
                else -> LiveStatus.Idle
            }
        }
    }

    fun open() {
        bridge.bindListener(listener)
        bridge.attachCamera()
    }

    fun close() {
        bridge.detach()
        bridge.bindListener(null)
    }

    override fun start(ingestUrl: String, streamKey: String) {
        if (ingestUrl.isBlank() || streamKey.isBlank()) {
            _status.value = LiveStatus.Failed("Δεν έχει οριστεί κλειδί μετάδοσης YouTube.")
            return
        }
        bridge.startStream(ingestUrl, streamKey)
    }

    override fun stop() = bridge.stopStream()

    override fun switchCamera() = bridge.switchCamera()

    private fun failureMessage(reason: String?): String = when (reason) {
        "permission" -> "Η μετάδοση χρειάζεται άδεια για κάμερα και μικρόφωνο (Ρυθμίσεις → Πανόραμα)."
        "rejected" -> "Το YouTube απέρριψε το κλειδί μετάδοσης."
        "closed" -> "Η σύνδεση με το YouTube διακόπηκε."
        else -> "Η σύνδεση με το YouTube απέτυχε. Ελέγξτε το δίκτυο και δοκιμάστε ξανά."
    }
}
