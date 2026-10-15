package gr.panoramapolihnitou.app.media

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

sealed interface LiveStatus {
    data object Idle : LiveStatus
    data object Connecting : LiveStatus
    data object Live : LiveStatus
    data class Failed(val message: String) : LiveStatus
}

/**
 * Pushes the camera + microphone to an RTMP(S) ingest — the YouTube channel's
 * stream key, handed out by the backend only to codes with the `live` role.
 * Android: RootEncoder. iOS: HaishinKit, via the Swift bridge in iosApp.
 */
interface LiveBroadcaster {
    val status: StateFlow<LiveStatus>

    /** [ingestUrl] like `rtmps://a.rtmps.youtube.com/live2`; the key is appended as the stream name. */
    fun start(ingestUrl: String, streamKey: String)
    fun stop()
    fun switchCamera()
}

/**
 * Full-bleed camera preview that owns the broadcaster's lifecycle: asks for
 * camera + microphone permission, opens the camera, and hands the broadcaster
 * to [onReady]. Leaving the composition stops any running broadcast and
 * releases the camera. [onReady] receives null where live streaming isn't
 * available in this build.
 */
@Composable
expect fun LiveCameraPreview(
    modifier: Modifier,
    onReady: (LiveBroadcaster?) -> Unit,
    onError: (String) -> Unit
)
