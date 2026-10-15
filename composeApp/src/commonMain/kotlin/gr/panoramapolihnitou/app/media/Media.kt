package gr.panoramapolihnitou.app.media

import androidx.compose.runtime.Composable
import kotlin.random.Random

enum class AttachmentKind { IMAGE, AUDIO }

/**
 * A photo or voice recording held in memory until the contributor sends it.
 * Photos are already re-encoded as JPEG and downscaled to [MAX_IMAGE_DIMENSION],
 * so a dozen of them stay well inside the upload limit.
 */
class MediaAttachment(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val kind: AttachmentKind,
    val durationSeconds: Int? = null
) {
    /** Stable key for lists; file names can repeat across separate picks. */
    val id: String = Random.nextLong().toString(16)
}

/** Longest edge, in pixels, of every photo the pickers hand back. */
const val MAX_IMAGE_DIMENSION = 2048
const val JPEG_QUALITY = 85
const val MAX_GALLERY_PICK = 10

interface ImagePicker {
    /** Opens the camera; requests camera permission first where the platform needs it. */
    fun takePhoto()

    /** Opens the system photo picker (no storage permission needed on either platform). */
    fun pickFromGallery()
}

/**
 * Platform camera + photo-library launcher. Results arrive on the main thread as
 * JPEG [MediaAttachment]s; [onError] receives a Greek, user-facing message.
 */
@Composable
expect fun rememberImagePicker(
    onImages: (List<MediaAttachment>) -> Unit,
    onError: (String) -> Unit
): ImagePicker

interface AudioRecorder {
    /** Compose-observable. */
    val isRecording: Boolean

    /** Requests microphone permission if needed, then starts recording AAC audio. */
    fun start()

    /** Stops and delivers the recording through `onRecorded`. */
    fun stop()

    /** Stops and throws the recording away (e.g. when leaving the screen). */
    fun cancel()
}

@Composable
expect fun rememberAudioRecorder(
    onRecorded: (MediaAttachment) -> Unit,
    onError: (String) -> Unit
): AudioRecorder
