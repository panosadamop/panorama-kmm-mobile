@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package gr.panoramapolihnitou.app.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVEncoderBitRateKey
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.CoreAudioTypes.kAudioFormatMPEG4AAC
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.NSDate
import platform.Foundation.NSFileManager
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy
import kotlin.coroutines.resume
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

@Composable
actual fun rememberImagePicker(
    onImages: (List<MediaAttachment>) -> Unit,
    onError: (String) -> Unit
): ImagePicker {
    val scope = rememberCoroutineScope()
    val currentOnImages by rememberUpdatedState(onImages)
    val currentOnError by rememberUpdatedState(onError)
    return remember {
        IosImagePicker(scope, onImages = { currentOnImages(it) }, onError = { currentOnError(it) })
    }
}

/**
 * Camera via UIImagePickerController (prompts for camera access itself, using
 * NSCameraUsageDescription); library via PHPickerViewController, which runs
 * out-of-process and needs no photo-library permission at all.
 */
private class IosImagePicker(
    private val scope: CoroutineScope,
    private val onImages: (List<MediaAttachment>) -> Unit,
    private val onError: (String) -> Unit
) : ImagePicker {

    // UIKit holds delegates weakly — keep them alive while their picker is up.
    private var cameraDelegate: CameraDelegate? = null
    private var galleryDelegate: GalleryDelegate? = null

    override fun takePhoto() {
        val camera = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
        if (!UIImagePickerController.isSourceTypeAvailable(camera)) {
            onError("Η συσκευή δεν διαθέτει κάμερα.")
            return
        }
        val delegate = CameraDelegate { image ->
            cameraDelegate = null
            if (image != null) deliver(listOf(image))
        }
        cameraDelegate = delegate
        val picker = UIImagePickerController()
        picker.sourceType = camera
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    override fun pickFromGallery() {
        val configuration = PHPickerConfiguration()
        configuration.filter = PHPickerFilter.imagesFilter
        configuration.selectionLimit = MAX_GALLERY_PICK.toLong()
        val delegate = GalleryDelegate { results ->
            galleryDelegate = null
            if (results.isEmpty()) return@GalleryDelegate
            scope.launch {
                val images = results.mapNotNull { result -> result.loadImageData()?.let { UIImage(data = it) } }
                if (images.isEmpty()) onError("Δεν ήταν δυνατή η ανάγνωση των φωτογραφιών.")
                else deliver(images)
            }
        }
        galleryDelegate = delegate
        val picker = PHPickerViewController(configuration = configuration)
        picker.delegate = delegate
        topViewController()?.presentViewController(picker, animated = true, completion = null)
    }

    private fun deliver(images: List<UIImage>) {
        scope.launch {
            val stamp = NSDate().timeIntervalSince1970.toLong()
            val attachments = withContext(Dispatchers.Default) {
                images.mapIndexedNotNull { index, image ->
                    image.toJpegBytes()?.let { bytes ->
                        MediaAttachment(
                            fileName = "photo-$stamp-${index + 1}.jpg",
                            mimeType = "image/jpeg",
                            bytes = bytes,
                            kind = AttachmentKind.IMAGE
                        )
                    }
                }
            }
            if (attachments.isEmpty()) onError("Δεν ήταν δυνατή η ανάγνωση της φωτογραφίας.")
            else onImages(attachments)
        }
    }
}

private class CameraDelegate(
    private val onFinished: (UIImage?) -> Unit
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>
    ) {
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinished(image)
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinished(null)
    }
}

private class GalleryDelegate(
    private val onFinished: (List<PHPickerResult>) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {

    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onFinished(didFinishPicking.filterIsInstance<PHPickerResult>())
    }
}

private suspend fun PHPickerResult.loadImageData(): NSData? = suspendCancellableCoroutine { continuation ->
    itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
        continuation.resume(data)
    }
}

/**
 * Scales the long edge down to [MAX_IMAGE_DIMENSION] and re-encodes as JPEG.
 * Redrawing bakes in the orientation and drops every bit of metadata (incl. GPS).
 */
private fun UIImage.toJpegBytes(): ByteArray? {
    val (width, height) = size.useContents { width to height }
    if (width <= 0.0 || height <= 0.0) return null
    val scale = min(1.0, MAX_IMAGE_DIMENSION / max(width, height))
    val targetWidth = floor(width * scale)
    val targetHeight = floor(height * scale)

    UIGraphicsBeginImageContextWithOptions(CGSizeMake(targetWidth, targetHeight), true, 1.0)
    drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
    val normalized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()

    val jpeg = UIImageJPEGRepresentation(normalized ?: return null, JPEG_QUALITY / 100.0) ?: return null
    return jpeg.toByteArray()
}

@Composable
actual fun rememberAudioRecorder(
    onRecorded: (MediaAttachment) -> Unit,
    onError: (String) -> Unit
): AudioRecorder {
    val currentOnRecorded by rememberUpdatedState(onRecorded)
    val currentOnError by rememberUpdatedState(onError)
    val recorder = remember {
        IosAudioRecorder(onRecorded = { currentOnRecorded(it) }, onError = { currentOnError(it) })
    }
    DisposableEffect(recorder) { onDispose { recorder.cancel() } }
    return recorder
}

private class IosAudioRecorder(
    private val onRecorded: (MediaAttachment) -> Unit,
    private val onError: (String) -> Unit
) : AudioRecorder {

    override var isRecording by mutableStateOf(false)
        private set

    private var recorder: AVAudioRecorder? = null
    private var filePath: String? = null

    override fun start() {
        if (isRecording) return
        AVAudioSession.sharedInstance().requestRecordPermission { granted ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted) begin() else onError("Χρειάζεται άδεια χρήσης μικροφώνου για ηχογράφηση.")
            }
        }
    }

    private fun begin() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayAndRecord, null)
        session.setActive(true, null)

        val path = NSTemporaryDirectory() + "recording-${NSDate().timeIntervalSince1970.toLong()}.m4a"
        val settings = mapOf<Any?, Any?>(
            AVFormatIDKey to NSNumber(unsignedInt = kAudioFormatMPEG4AAC),
            AVSampleRateKey to NSNumber(double = 44_100.0),
            AVNumberOfChannelsKey to NSNumber(int = 1),
            AVEncoderBitRateKey to NSNumber(int = 64_000)
        )
        val newRecorder = AVAudioRecorder(NSURL.fileURLWithPath(path), settings, null)
        if (!newRecorder.record()) {
            session.setActive(false, null)
            onError("Δεν ήταν δυνατή η έναρξη της ηχογράφησης.")
            return
        }
        recorder = newRecorder
        filePath = path
        isRecording = true
    }

    override fun stop() {
        val active = recorder ?: return
        val duration = active.currentTime
        finish()
        val path = filePath ?: return
        filePath = null
        val data = NSFileManager.defaultManager.contentsAtPath(path)
        NSFileManager.defaultManager.removeItemAtPath(path, null)
        if (data == null || data.length.toInt() == 0) {
            onError("Η ηχογράφηση ήταν πολύ σύντομη. Δοκιμάστε ξανά.")
            return
        }
        onRecorded(
            MediaAttachment(
                fileName = path.substringAfterLast('/'),
                mimeType = "audio/mp4",
                bytes = data.toByteArray(),
                kind = AttachmentKind.AUDIO,
                durationSeconds = duration.toInt()
            )
        )
    }

    override fun cancel() {
        finish()
        filePath?.let { NSFileManager.defaultManager.removeItemAtPath(it, null) }
        filePath = null
    }

    private fun finish() {
        val active = recorder ?: return
        recorder = null
        isRecording = false
        active.stop()
        AVAudioSession.sharedInstance().setActive(false, null)
    }
}

internal fun topViewController(): UIViewController? {
    var controller = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (controller?.presentedViewController != null) {
        controller = controller.presentedViewController
    }
    return controller
}

internal fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val bytes = ByteArray(size)
    if (size > 0) {
        bytes.usePinned { pinned -> memcpy(pinned.addressOf(0), this.bytes, length) }
    }
    return bytes
}
