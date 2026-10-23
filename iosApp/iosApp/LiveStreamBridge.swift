import AVFoundation
import ComposeApp
import HaishinKit
import RTMPHaishinKit
import UIKit

/// Implements the Kotlin `IosLiveStreamBridge`
/// (composeApp/src/iosMain/.../media/LiveBroadcast.ios.kt) with HaishinKit 2.2,
/// which is Swift-only and so can't be called from Kotlin directly. Publishes the
/// camera + microphone over RTMP(S) to the YouTube ingest URL and stream key the
/// backend hands to contributors with the `live` role.
///
/// Kotlin calls in on the main thread. HaishinKit 2's mixer, connection and stream
/// are actors, so every operation on them goes through [enqueue] and runs strictly
/// in call order — e.g. a quick close → reopen of the screen can't attach the
/// camera before the previous detach finished. State the UI reads (`isPublishing`)
/// is only touched on the main thread.
final class HaishinKitLiveStreamBridge: NSObject, IosLiveStreamBridge {

    private let mixer = MediaMixer()
    private let connection = RTMPConnection()
    private lazy var stream = RTMPStream(connection: connection)
    private var listener: IosLiveStreamListener?
    private var position: AVCaptureDevice.Position = .back
    private var previewView: MTHKView?
    private var isPublishing = false
    private var statusTask: Task<Void, Never>?
    private var pending: Task<Void, Never>?

    func makePreviewView() -> UIView {
        let view = MTHKView(frame: .zero)
        view.videoGravity = .resizeAspectFill
        previewView = view
        enqueue { [mixer] in await mixer.addOutput(view) }
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
        UIApplication.shared.isIdleTimerDisabled = true
        emit("connecting", reason: nil)
        watchConnection()
        enqueue { [connection, stream, weak self] in
            do {
                _ = try await connection.connect(url)
                _ = try await stream.publish(streamKey)
                await MainActor.run {
                    guard self?.isPublishing == true else { return }
                    self?.emit("live", reason: nil)
                }
            } catch RTMPStream.Error.requestFailed(let response)
                where response.status?.code == RTMPStream.Code.publishBadName.rawValue {
                await self?.fail("rejected")
            } catch {
                await self?.fail("connect")
            }
        }
    }

    func stopStream() {
        guard isPublishing else { return }
        tearDownConnection()
        emit("idle", reason: nil)
    }

    func switchCamera() {
        position = position == .back ? .front : .back
        let camera = Self.camera(at: position)
        enqueue { [mixer] in try? await mixer.attachVideo(camera) }
    }

    func detach() {
        if isPublishing { tearDownConnection() }
        let view = previewView
        previewView = nil
        enqueue { [mixer, stream] in
            await mixer.stopRunning()
            try? await mixer.attachVideo(nil)
            try? await mixer.attachAudio(nil)
            await mixer.removeOutput(stream)
            if let view { await mixer.removeOutput(view) }
            try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        }
    }

    func bindListener(listener: IosLiveStreamListener?) {
        self.listener = listener
    }

    // MARK: - Private

    private func startCapture() {
        let session = AVAudioSession.sharedInstance()
        try? session.setCategory(.playAndRecord, mode: .videoRecording, options: [.defaultToSpeaker])
        try? session.setActive(true)

        let camera = Self.camera(at: position)
        enqueue { [mixer, stream] in
            await mixer.setSessionPreset(.hd1280x720)
            await mixer.setVideoOrientation(.portrait)
            try? await mixer.attachAudio(AVCaptureDevice.default(for: .audio))
            try? await mixer.attachVideo(camera)
            try? await mixer.setFrameRate(30)
            // 720p portrait at 2.5 Mbps: inside YouTube's 720p30 range and gentle on 4G uplinks.
            try? await stream.setVideoSettings(VideoCodecSettings(
                videoSize: CGSize(width: 720, height: 1280),
                bitRate: 2_500_000,
                expectedFrameRate: 30
            ))
            await mixer.addOutput(stream)
            await mixer.startRunning()
        }
    }

    /// A drop after going live arrives as `NetConnection.Connect.Closed`; our own
    /// close produces the same code, but by then `isPublishing` is already false.
    private func watchConnection() {
        statusTask?.cancel()
        statusTask = Task { [connection, weak self] in
            for await status in await connection.status
                where status.code == RTMPConnection.Code.connectClosed.rawValue {
                await self?.fail("closed")
            }
        }
    }

    private func tearDownConnection() {
        isPublishing = false
        statusTask?.cancel()
        statusTask = nil
        UIApplication.shared.isIdleTimerDisabled = false
        enqueue { [connection, stream] in
            _ = try? await stream.close()
            try? await connection.close()
        }
    }

    @MainActor
    private func fail(_ reason: String) {
        guard isPublishing else { return }
        tearDownConnection()
        emit("error", reason: reason)
    }

    private func enqueue(_ operation: @escaping () async -> Void) {
        let previous = pending
        pending = Task {
            await previous?.value
            await operation()
        }
    }

    private func emit(_ status: String, reason: String?) {
        DispatchQueue.main.async {
            self.listener?.onStatus(status: status, reason: reason)
        }
    }

    private func requestAccess(for mediaType: AVMediaType, completion: @escaping (Bool) -> Void) {
        AVCaptureDevice.requestAccess(for: mediaType) { granted in
            DispatchQueue.main.async { completion(granted) }
        }
    }

    private static func camera(at position: AVCaptureDevice.Position) -> AVCaptureDevice? {
        AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: position)
    }
}
