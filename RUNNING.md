# Running, Releasing & Publishing — Panorama Πολιχνίτου

This guide is written for a **Windows 11** development machine (with notes for the
Mac steps that iOS requires). It covers four things:

1. [Prerequisites & one-time setup](#1-prerequisites--one-time-setup)
2. [Running locally for development](#2-running-locally-for-development)
3. [Creating dev releases (shareable test builds)](#3-creating-dev-releases)
4. [Publishing to Google Play & the Apple App Store](#4-publishing-to-the-stores)

> **Platform reality check:** Android is fully buildable on Windows. iOS **requires a
> Mac** (Xcode + Apple toolchain). Everything for iOS is in the repo and will build on
> a Mac unchanged — the iOS sections below are written so a colleague on a Mac (or a
> CI Mac runner) can pick them up directly.

---

## 1. Prerequisites & one-time setup

### 1.1 Android (Windows)

| Tool | Version | Notes |
|---|---|---|
| **Android Studio** | Ladybug (2024.2) or newer — Meerkat recommended | Bundles a JDK 17 and the Android SDK |
| **JDK** | 17 (bundled with Android Studio) | Only needed separately for command-line Gradle |
| **Android SDK** | API 34 (compileSdk) + build-tools | Installed via Android Studio SDK Manager |
| A device or emulator | API 24+ | Enable *USB debugging* on a physical phone |

**Steps**

1. Install **Android Studio** and, during setup, install the **Android SDK Platform 34**
   and an emulator image (e.g. *Pixel 7, API 34*).
2. Open the `panorama-app/` folder (the one containing `settings.gradle.kts`).
3. Wait for **Gradle sync**. On first sync Android Studio:
   - downloads all dependencies from the version catalog,
   - **generates the Gradle wrapper JAR** (`gradle/wrapper/gradle-wrapper.jar`),
   - creates `local.properties` pointing at your SDK.
4. If sync complains it can't find the SDK, copy `local.properties.sample` to
   `local.properties` and set `sdk.dir` (see that file for the Windows path format).

> **Command-line Gradle without Android Studio?** Install a JDK 17 and Gradle 8.9, then
> run `gradle wrapper --gradle-version 8.9` once in the project root to create the
> wrapper JAR. After that, `./gradlew` (Git Bash) or `gradlew.bat` (cmd/PowerShell) work.

### 1.2 iOS (Mac only)

| Tool | Version |
|---|---|
| macOS | 13+ |
| Xcode | 15+ |
| Command Line Tools | matching Xcode |
| CocoaPods | *not required* — this project links the framework directly, no Pods |

Run `brew install kdoctor && kdoctor` on the Mac to verify the KMP toolchain is healthy.

---

## 2. Running locally for development

### 2.1 Android — from Android Studio (recommended on Windows)

1. In the toolbar, pick the **`composeApp`** run configuration.
2. Pick a device (emulator or a connected phone in USB-debug mode).
3. Press **Run ▶** (or **Debug 🐞**).

Compose **Live Edit** / hot reload works for UI tweaks while the app runs.

### 2.2 Android — from the command line

From the project root:

```powershell
# PowerShell / cmd
.\gradlew.bat :composeApp:installDebug     # build + install the debug app on the running device/emulator
```

```bash
# Git Bash
./gradlew :composeApp:installDebug
```

Then launch it from the launcher, or:

```powershell
adb shell am start -n gr.panoramapolihnitou.app.debug/gr.panoramapolihnitou.app.MainActivity
```

> The **debug** build uses the application id `gr.panoramapolihnitou.app.debug`
> (suffix `.debug`) so it can be installed **side-by-side** with a release build.

Useful commands:

```powershell
.\gradlew.bat :composeApp:assembleDebug    # produce a debug APK (no install)
.\gradlew.bat clean                        # clean build outputs
.\gradlew.bat :composeApp:dependencies     # inspect the dependency tree
```

Debug APK output:
`composeApp/build/outputs/apk/debug/composeApp-debug.apk`

### 2.3 iOS — on a Mac

**Option A — Android Studio + KMP plugin (simplest):**
Install the *Kotlin Multiplatform* plugin, open the project, choose the **iosApp**
run configuration and a simulator, then Run.

**Option B — Xcode:**
1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the **iosApp** scheme + a simulator.
3. Press **Run ▶**. The "Compile Kotlin Framework" build phase runs
   `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` automatically to build
   and embed the shared `ComposeApp` framework.

> If Xcode reports the project is damaged, regenerate the iOS app from the official
> [Kotlin Multiplatform wizard](https://kmp.jetbrains.com/) and drop the
> `iosApp/iosApp/*.swift` + `Info.plist` from this repo into it — the Kotlin/Swift
> interop (`MainViewControllerKt.MainViewController()`) is unchanged.

### 2.4 Changing the backend URL

All networking config lives in one place:
`composeApp/src/commonMain/kotlin/gr/panoramapolihnitou/app/data/remote/HttpClientFactory.kt`
→ `ApiConfig.HOST` / `ApiConfig.BASE_PATH`.

---

## 3. Creating dev releases

"Dev releases" = installable builds you hand to testers before going to the stores.

### 3.1 Android — quick shareable APK (debug)

The fastest option. No signing setup needed; anyone can sideload it.

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Share: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Testers must allow *Install unknown apps* on their device.

### 3.2 Android — signed release APK/AAB

Play Store and "proper" test builds need a **signed release**.

**Step 1 — create an upload keystore (once):**

```powershell
# Uses keytool from the JDK bundled with Android Studio.
# Adjust the path to your Android Studio JBR if keytool isn't on PATH.
& "$env:ProgramFiles\Android\Android Studio\jbr\bin\keytool.exe" `
  -genkeypair -v `
  -keystore release-keystore.jks `
  -alias panorama `
  -keyalg RSA -keysize 2048 -validity 10000
```

Answer the prompts and **remember the passwords**. Keep `release-keystore.jks` safe and
**out of git** (already in `.gitignore`).

**Step 2 — wire it up:** copy `keystore.properties.sample` → `keystore.properties` and
fill in the passwords/paths. The `composeApp/build.gradle.kts` release block reads this
file automatically if it exists.

**Step 3 — build:**

```powershell
.\gradlew.bat :composeApp:assembleRelease      # signed APK  → build/outputs/apk/release/
.\gradlew.bat :composeApp:bundleRelease        # signed AAB  → build/outputs/bundle/release/
```

- **APK** (`composeApp-release.apk`) — for direct sideloading / Firebase App Distribution.
- **AAB** (`composeApp-release.aab`) — the format Google Play requires.

### 3.3 Android — distributing to testers

- **Firebase App Distribution** (recommended for teams): upload the APK/AAB, invite
  testers by email — they get a link and install over the air.
- **Google Play — Internal testing track**: upload the AAB (see §4), add tester emails,
  share the opt-in link. Closest to production.

### 3.4 iOS — dev/test builds (Mac)

- **Simulator/device via Xcode:** just Run (§2.3) onto a registered device.
- **TestFlight** (the standard way to share iOS test builds):
  1. In Xcode: **Product ▸ Archive** (scheme = iosApp, target = *Any iOS Device*).
  2. In the Organizer, **Distribute App ▸ TestFlight (App Store Connect)**.
  3. Add testers in App Store Connect → TestFlight. Internal testers get builds
     immediately; external testers require a light Beta App Review.

---

## 4. Publishing to the stores

### 4.0 Set the version before every release

Bump these together for each store submission:

- **Android:** `composeApp/build.gradle.kts` → `versionCode` (integer, must increase
  every upload) and `versionName` (e.g. `"1.0.1"`).
- **iOS:** `iosApp/iosApp/Info.plist` → `CFBundleVersion` (build number, must increase)
  and `CFBundleShortVersionString` (marketing version).

---

### 4.1 Google Play

**Prerequisites**
- A **Google Play Console** account (one-time $25 fee).
- A signed **AAB** (§3.2).
- Store assets: app icon (512×512 PNG), feature graphic (1024×500), 2–8 phone
  screenshots, short + full description, privacy policy URL.

**Recommended: Play App Signing.** Let Google manage the app signing key; you keep only
the *upload* key created in §3.2. This is the default when you create the app.

**Steps**
1. **Play Console → Create app.** Set name (*Panorama Πολιχνίτου*), default language
   (Greek), app/game = App, free.
2. Complete **Dashboard → Set up your app**: privacy policy, ads declaration, content
   rating questionnaire, target audience, data safety form (the app only reads public
   content and stores bookmarks locally → declare accordingly).
3. **Release → Testing → Internal testing → Create release.**
   - Upload `composeApp-release.aab`.
   - Add release notes. Save → Review → **Start rollout to Internal testing**.
   - Add tester emails and share the opt-in URL. Verify on a real device.
4. Promote the same build up the tracks: **Internal → Closed → Open → Production**.
5. **Production → Create release**, upload (or promote) the AAB, roll out. First
   production submission triggers a Google review (hours to a few days).

**CLI alternative (CI):** use [`fastlane supply`](https://docs.fastlane.tools/actions/supply/)
or the Google Play Developer API with a service account to upload AABs automatically.

---

### 4.2 Apple App Store (Mac required)

**Prerequisites**
- **Apple Developer Program** membership ($99/year).
- Xcode signed in with your Apple ID (**Xcode ▸ Settings ▸ Accounts**).
- An **App ID / bundle identifier** — this project uses `gr.panoramapolihnitou.app`
  (set via `iosApp/Configuration/Config.xcconfig` → `BUNDLE_ID`).
- Store assets: 1024×1024 icon (no alpha), screenshots for required device sizes,
  description, keywords, privacy policy URL.

**Steps**
1. **App Store Connect → My Apps → +** → **New App**. Platform iOS, pick the bundle id,
   set name and primary language (Greek).
2. In Xcode set your **Team** (`Config.xcconfig → TEAM_ID`) and confirm automatic
   signing resolves the provisioning profile.
3. **Product ▸ Archive** (target = *Any iOS Device (arm64)*).
4. In the **Organizer**, **Distribute App ▸ App Store Connect ▸ Upload**.
5. In App Store Connect, complete the app record: screenshots, description, keywords,
   support URL, privacy policy, **App Privacy** answers (reads public content; bookmarks
   stored on-device; no tracking).
6. Attach the uploaded build to the version, submit for **App Review**. Approval
   typically takes ~24–48h; then release manually or automatically.

**CLI alternative (CI):** [`fastlane deliver`](https://docs.fastlane.tools/actions/deliver/)
or `xcrun altool`/`notarytool` for uploads.

---

## 5. Troubleshooting

| Symptom | Fix |
|---|---|
| Gradle sync fails: *"wrapper jar missing"* | Let Android Studio sync once, or run `gradle wrapper --gradle-version 8.9`. |
| *"SDK location not found"* | Create `local.properties` with `sdk.dir` (see `local.properties.sample`). |
| App shows the retry screen | Device offline, or the WordPress API is unreachable — check `https://panoramapolihnitou.gr/wp-json/wp/v2/posts` in a browser. |
| Images don't load | Confirm posts have a *featured image*; the app falls back to a placeholder icon otherwise. |
| iOS: *"ComposeApp framework not found"* | Ensure the "Compile Kotlin Framework" build phase ran; on a Mac run `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` once. |
| Release build not signed | Create `keystore.properties` from the sample (§3.2). Without it, `assembleRelease` produces an **unsigned** APK. |
| `adb` not found | Add `%LOCALAPPDATA%\Android\Sdk\platform-tools` to your PATH. |

---

## 6. Handy command reference

```powershell
# Android (Windows PowerShell)
.\gradlew.bat :composeApp:installDebug       # run debug on device/emulator
.\gradlew.bat :composeApp:assembleDebug      # debug APK
.\gradlew.bat :composeApp:assembleRelease    # signed release APK
.\gradlew.bat :composeApp:bundleRelease      # signed release AAB (for Play)
.\gradlew.bat clean                          # clean

# iOS (Mac)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode   # build shared framework
# then Archive & distribute from Xcode
```
