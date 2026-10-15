import AVFoundation
import ComposeApp
import HaishinKit
import UIKit

/// Implements the Kotlin `IosLiveStreamBridge`
/// (composeApp/src/iosMain/.../media/LiveBroadcast.ios.kt) with HaishinKit 1.9,
/// which is Swift-only and so can't be called from Kotlin directly. Publishes the
/// camera + microphone over RTMP(S) to the YouTube ingest URL and stream key the
/// backend hands to contributors with the `live` role.
final class HaishinKitLiveStreamBridge: NSObject, IosLiveStreamBridge {

    private let connection = RTMPConnection()
    private lazy var stream = RTMPStream(connection: connection)
    private var listener: IosLiveStreamListener?
    private var position: AVCaptureDevice.Position = .back
    private var streamKey: String?
    private var isPublishing = false

    func makePreviewView() -> UIView {
        let view = MTHKView(frame: .zero)
        view.videoGravity = .resizeAspectFill
        view.attachStream(stream)
        return view
    }

    func attachCamera() {
        requestAccess(for: .video) { [weak self] videoGranted in
            self?.requestAccess(for: .audio) { audioGranted in
                guard let self else { return }
                guard videoGranted && audioGranted else {
                    self.emit("error", reason: "permission")
                    return
                }
                self.startCapture()
            }
        }
    }

    func startStream(url: String, streamKey: String) {
        guard !isPublishing else { return }
        isPublishing = true
        self.streamKey = streamKey
        UIApplication.shared.isIdleTimerDisabled = true
        emit("connecting", reason: nil)
        connection.addEventListener(.rtmpStatus, selector: #selector(rtmpStatusHandler(_:)), observer: self)
        connection.addEventListener(.ioError, selector: #selector(rtmpErrorHandler(_:)), observer: self)
        connection.connect(url)
    }

    func stopStream() {
        guard isPublishing else { return }
        tearDownConnection()
        emit("idle", reason: nil)
    }

    func switchCamera() {
        position = position == .back ? .front : .back
        stream.attachCamera(AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position))
    }

    func detach() {
        if isPublishing { tearDownConnection() }
        stream.attachCamera(nil)
        stream.attachAudio(nil)
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
    }

    func bindListener(listener: IosLiveStreamListener?) {
        self.listener = listener
    }

    // MARK: - Private

    private func startCapture() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .videoRecording, options: [.defaultToSpeaker])
        try? session.setActive(true)

        // 720p portrait at 2.5 Mbps: inside YouTube's 720p30 range and gentle on 4G uplinks.
        stream.sessionPreset = .hd1280x720
        stream.frameRate = 30
        stream.videoOrientation = .portrait
        stream.videoSettings = VideoCodecSettings(
            videoSize: CGSize(width: 720, height: 1280),
            bitRate: 2_500_000
        )
        stream.attachAudio(AVCaptureDevice.default(for: .audio))
        stream.attachCamera(AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position))
    }

    private func requestAccess(for mediaType: AVMediaType, completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: mediaType) { granted in
            DispatchQueue.main.async { completion(granted) }
        }
    }

    private func tearDownConnection() {
        isPublishing = false
        connection.removeEventListener(.rtmpStatus, selector: #selector(rtmpStatusHandler(_:)), observer: self)
        connection.removeEventListener(.ioError, selector: #selector(rtmpErrorHandler(_:)), observer: self)
        stream.close()
        connection.close()
        UIApplication.shared.isIdleTimerDisabled = false
    }

    private func fail(_ reason: String) {
        DispatchQueue.main.async {
            guard self.isPublishing else { return }
            self.tearDownConnection()
            self.emit("error", reason: reason)
        }
    }

    private func emit(_ status: String, reason: String?) {
        DispatchQueue.main.async {
            self.listener?.onStatus(status: status, reason: reason)
        }
    }

    @objc private func rtmpStatusHandler(_ notification: Notification) {
        let event = Event.from(notification)
        guard let data = event.data as? ASObject, let code = data["code"] as? String else { return }
        switch code {
        case RTMPConnection.Code.connectSuccess.rawValue:
            stream.publish(streamKey)
        case RTMPStream.Code.publishStart.rawValue:
            emit("live", reason: nil)
        case RTMPStream.Code.publishBadName.rawValue:
            fail("rejected")
        case RTMPConnection.Code.connectFailed.rawValue,
             RTMPConnection.Code.connectRejected.rawValue:
            fail("connect")
        case RTMPConnection.Code.connectClosed.rawValue:
            fail("closed")
        default:
            break
        }
    }

    @objc private func rtmpErrorHandler(_ notification: Notification) {
        fail("connect")
    }
}
