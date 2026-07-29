import SwiftUI
import FirebaseCore
import FirebaseMessaging
import UserNotifications

/// All devices subscribe to this topic — no per-device token registry needed
/// since every install should hear about new articles. Must match the Android
/// topic name exactly (see PanoramaMessagingService.kt). See RUNNING.md →
/// "Push notifications" for how the WordPress site triggers a send on publish.
private let newArticlesTopic = "new_articles"

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Firebase throws if GoogleService-Info.plist is missing — safe to skip
        // entirely until a real one is dropped in from the Firebase console
        // (mirrors the Android side, which no-ops the same way without
        // google-services.json).
        guard Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist") != nil else {
            return true
        }

        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in }
        application.registerForRemoteNotifications()
        Messaging.messaging().subscribe(toTopic: newArticlesTopic)

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }

    // Show the notification banner even while the app is in the foreground.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .list])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.all) // Compose handles its own insets.
        }
    }
}
