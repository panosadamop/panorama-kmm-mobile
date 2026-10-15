package gr.panoramapolihnitou.app.media

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Must match the `<provider>` authority in AndroidManifest.xml. */
private fun fileProviderAuthority(context: Context) = "${context.packageName}.fileprovider"

@Composable
actual fun rememberImagePicker(
    onImages: (List<MediaAttachment>) -> Unit,
    onError: (String) -> Unit
): ImagePicker {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnImages by rememberUpdatedState(onImages)
    val currentOnError by rememberUpdatedState(onError)
    // Survives the activity being recreated while the camera app is in front.
    var pendingCapturePath by rememberSaveable { mutableStateOf<String?>(null) }

    fun deliver(uris: List<Uri>, deleteAfter: File? = null) {
        if (uris.isEmpty()) return
        scope.launch {
            val images = withContext(Dispatchers.IO) {
                uris.mapIndexedNotNull { index, uri ->
                    encodeJpeg(context, uri)?.let { bytes ->
                        MediaAttachment(
                            fileName = "photo-${System.currentTimeMillis()}-${index + 1}.jpg",
                            mimeType = "image/jpeg",
                            bytes = bytes,
                            kind = AttachmentKind.IMAGE
                        )
                    }
                }.also { deleteAfter?.delete() }
            }
            if (images.isEmpty()) currentOnError("Δεν ήταν δυνατή η ανάγνωση της φωτογραφίας.")
            else currentOnImages(images)
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingCapturePath?.let(::File)
        pendingCapturePath = null
        if (saved && file != null) deliver(listOf(Uri.fromFile(file)), deleteAfter = file)
        else file?.delete()
    }
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(MAX_GALLERY_PICK)
    ) { uris -> deliver(uris) }

    fun launchCamera() {
        val file = File(context.cacheDir, "captures/photo-${System.currentTimeMillis()}.jpg")
        file.parentFile?.mkdirs()
        pendingCapturePath = file.absolutePath
        val uri = FileProvider.getUriForFile(context, fileProviderAuthority(context), file)
        runCatching { takePicture.launch(uri) }
            .onFailure { currentOnError("Δεν βρέθηκε εφαρμογή κάμερας.") }
    }

    // CAMERA is declared in the manifest (for live streaming), and once it is,
    // ACTION_IMAGE_CAPTURE refuses to start until the permission is granted.
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
        else currentOnError("Χρειάζεται άδεια χρήσης κάμερας για να τραβήξετε φωτογραφία.")
    }

    return object : ImagePicker {
        override fun takePhoto() {
            if (context.hasPermission(Manifest.permission.CAMERA)) launchCamera()
            else cameraPermission.launch(Manifest.permission.CAMERA)
        }

        override fun pickFromGallery() {
            pickImages.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    }
}

@Composable
actual fun rememberAudioRecorder(
    onRecorded: (MediaAttachment) -> Unit,
    onError: (String) -> Unit
): AudioRecorder {
    val context = LocalContext.current
    val currentOnRecorded by rememberUpdatedState(onRecorded)
    val currentOnError by rememberUpdatedState(onError)
    val recorder = remember {
        AndroidAudioRecorder(
            context = context.applicationContext,
            onRecorded = { currentOnRecorded(it) },
            onError = { currentOnError(it) }
        )
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recorder.begin()
        else currentOnError("Χρειάζεται άδεια χρήσης μικροφώνου για ηχογράφηση.")
    }
    recorder.requestPermission = { micPermission.launch(Manifest.permission.RECORD_AUDIO) }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}

private class AndroidAudioRecorder(
    private val context: Context,
    private val onRecorded: (MediaAttachment) -> Unit,
    private val onError: (String) -> Unit
) : AudioRecorder {

    override var isRecording by mutableStateOf(false)
        private set

    var requestPermission: () -> Unit = {}
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt = 0L

    override fun start() {
        if (isRecording) return
        if (context.hasPermission(Manifest.permission.RECORD_AUDIO)) begin() else requestPermission()
    }

    fun begin() {
        val file = File(context.cacheDir, "recordings/recording-${System.currentTimeMillis()}.m4a")
        file.parentFile?.mkdirs()
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
        else @Suppress("DEPRECATION") MediaRecorder()
        runCatching {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioChannels(1)
            recorder.setAudioSamplingRate(44_100)
            recorder.setAudioEncodingBitRate(64_000)
            recorder.setOutputFile(file.absolutePath)
            recorder.prepare()
            recorder.start()
        }.onSuccess {
            mediaRecorder = recorder
            outputFile = file
            startedAt = SystemClock.elapsedRealtime()
            isRecording = true
        }.onFailure {
            recorder.release()
            file.delete()
            onError("Δεν ήταν δυνατή η έναρξη της ηχογράφησης.")
        }
    }

    override fun stop() {
        val file = outputFile
        val stopped = finish()
        if (file == null) return
        val bytes = if (stopped) runCatching { file.readBytes() }.getOrNull() else null
        file.delete()
        if (bytes == null || bytes.isEmpty()) {
            onError("Η ηχογράφηση ήταν πολύ σύντομη. Δοκιμάστε ξανά.")
            return
        }
        onRecorded(
            MediaAttachment(
                fileName = file.name,
                mimeType = "audio/mp4",
                bytes = bytes,
                kind = AttachmentKind.AUDIO,
                durationSeconds = ((SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            )
        )
    }

    override fun cancel() {
        finish()
        outputFile?.delete()
        outputFile = null
    }

    /** False when MediaRecorder had nothing usable (it throws if stopped right after starting). */
    private fun finish(): Boolean {
        val recorder = mediaRecorder ?: return false
        mediaRecorder = null
        isRecording = false
        val ok = runCatching { recorder.stop() }.isSuccess
        recorder.release()
        return ok
    }
}

internal fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/**
 * Decodes [uri] with sub-sampling (camera originals can be 50+ MP), applies the
 * EXIF rotation, scales the long edge down to [MAX_IMAGE_DIMENSION] and
 * re-encodes as JPEG. Strips all other EXIF metadata — including GPS — on the way.
 */
private fun encodeJpeg(context: Context, uri: Uri): ByteArray? = runCatching {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_IMAGE_DIMENSION) sample *= 2
    val decoded = resolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null

    val rotation = resolver.openInputStream(uri)?.use { stream ->
        when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    } ?: 0f

    val scale = minOf(1f, MAX_IMAGE_DIMENSION.toFloat() / max(decoded.width, decoded.height))
    val matrix = Matrix().apply {
        if (scale < 1f) postScale(scale, scale)
        if (rotation != 0f) postRotate(rotation)
    }
    val output = if (matrix.isIdentity) decoded
    else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

    ByteArrayOutputStream().use { buffer ->
        output.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, buffer)
        if (output !== decoded) output.recycle()
        decoded.recycle()
        buffer.toByteArray()
    }
}.getOrNull()
