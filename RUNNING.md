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
| **Android Studio** | Meerkat (2024.3) or newer | Needs to understand AGP 8.11 |
| **JDK** | 17 or 21 | Bundled with Android Studio; needed separately for command-line Gradle |
| **Android SDK** | API 36 (compileSdk) + build-tools 36 | Installed via Android Studio SDK Manager |
| **Gradle** | 8.13 (via the wrapper) | AGP 8.11.2 requires Gradle 8.13+ |
| A device or emulator | API 24+ | Enable *USB debugging* on a physical phone |

> **Why API 36:** Google Play requires new apps and updates to target API 36
> from **31 August 2026**. `compileSdk`/`targetSdk` are set to 36 in
> `gradle/libs.versions.toml`.

**Steps**

1. Install **Android Studio** and, during setup, install the **Android SDK Platform 36**
   and an emulator image (e.g. *Pixel 8, API 36*).
2. Open the `panorama-app/` folder (the one containing `settings.gradle.kts`).
3. Wait for **Gradle sync**. On first sync Android Studio:
   - downloads all dependencies from the version catalog,
   - **generates the Gradle wrapper JAR** (`gradle/wrapper/gradle-wrapper.jar`),
   - creates `local.properties` pointing at your SDK.
4. If sync complains it can't find the SDK, copy `local.properties.sample` to
   `local.properties` and set `sdk.dir` (see that file for the Windows path format).

> **Command-line Gradle without Android Studio?** Install a JDK 17/21 and Gradle 8.13,
> then run `gradle wrapper --gradle-version 8.13` once in the project root to create the
> wrapper JAR. After that, `./gradlew` (Git Bash) or `gradlew.bat` (cmd/PowerShell) work.
> Set `JAVA_HOME` to the JDK if Gradle can't find one, e.g.
> `JAVA_HOME="C:/Program Files/Java/jdk-21.0.11" ./gradlew :composeApp:bundleRelease`.

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

### 3.0 Preview APK — one command (recommended for on-device testing)

Produces a **signed, self-contained APK** you can copy to any phone and install by
tapping it — **no Developer Options, no USB debugging, no `adb`** required (the phone
just needs "Install unknown apps" allowed for the app you open the file with).

```powershell
.\build-preview-apk.bat
```

Output: **`dist\panorama-preview.apk`**. Transfer it to the phone (USB copy, Google
Drive, email, etc.) and tap to install.

It signs with whatever `keystore.properties` points at — currently the real upload
keystore `release-keystore.jks` (§3.2), so preview APKs and Play uploads share the
same signature. Keep that file and its password backed up and out of git.

### 3.1 Android — quick shareable APK (debug)

The fastest option. No signing setup needed; anyone can sideload it.

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

Share: `composeApp/build/outputs/apk/debug/composeApp-debug.apk`
Testers must allow *Install unknown apps* on their device.

### 3.2 Android — signed release APK/AAB

Play Store and "proper" test builds need a **signed release**.

> **Already done on this machine.** `release-keystore.jks` (RSA 2048, alias
> `panorama`, valid until 2053) and `keystore.properties` exist in the project
> root and are **excluded from git**. ⚠️ **Back both up offline now** (password
> manager + encrypted backup): if you lose them you cannot ship updates signed
> with the same upload key and have to ask Google to reset it. The steps below
> are only needed on a new machine or to create a fresh key.

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
.\build-play-bundle.bat                        # signed AAB → dist\panorama-release.aab
.\gradlew.bat :composeApp:assembleRelease      # signed APK  → build/outputs/apk/release/
.\gradlew.bat :composeApp:bundleRelease        # signed AAB  → build/outputs/bundle/release/
```

- **APK** (`composeApp-release.apk`) — for direct sideloading / Firebase App Distribution.
- **AAB** (`composeApp-release.aab`) — the format Google Play requires.

`build-play-bundle.bat` is the one-command path: it checks that
`keystore.properties` exists, picks a JDK, builds the bundle and copies it to
`dist\panorama-release.aab`.

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
  and `CFBundleShortVersionString` (marketing version). Copy both values into
  `iosApp/PanoramaWidget/Info.plist` too — App Store Connect rejects an upload
  where the widget extension's `CFBundleVersion` doesn't match the app's.

---

### 4.1 Google Play

**Prerequisites**
- A **Google Play Console** account (one-time $25 fee).
- A signed **AAB** — already built: `dist\panorama-release.aab` (§3.2).
- Store assets — already prepared in `app-previews/play-store/`:
  - `play-icon-512.png` (512×512 app icon)
  - `feature-graphic-1024x500.png`
  - `screenshots/phone/01…05` (5 phone screenshots, 1280×2600)
  - `play-store-listing.md` — every text field + questionnaire answer to paste
  - privacy policy: `app-previews/privacy-policy/privacy-policy-el.html` (+ `PUBLISHING.md`)
- A **public privacy policy URL** (mandatory). Publish `app-previews/privacy-policy/privacy-policy-el.html` (see `PUBLISHING.md` there) on
  panoramapolihnitou.gr first; you cannot submit without the URL.

**Recommended: Play App Signing.** Let Google manage the app signing key; you keep only
the *upload* key created in §3.2. This is the default when you create the app.

**Steps**
1. **Play Console → Create app.** Name *Πανόραμα Πολιχνίτου*, default language
   **Greek (el-GR)**, app/game = App, free.
2. Complete **Dashboard → Set up your app** using the answers in
   `app-previews/play-store/play-store-listing.md`: privacy policy URL, app
   access, **ads = No** (the Ads SDK is not linked), content rating questionnaire,
   target audience (18+), news-publisher details, and the data safety form.
   **From 1.1.0 the answers change.** *App access*: "Some functionality is
   restricted"; give a demo access code. *Data safety*: declare Device or other IDs
   (FCM, push) plus Photos, Audio and Other user-generated content (contributor
   area only, optional, encrypted in transit). See `STORE-READINESS.md` §2.
3. **Main store listing:** paste the short/full description and upload the icon,
   feature graphic and the 5 phone screenshots.
4. **Release → Testing → Internal testing → Create release.**
   - Upload `dist\panorama-release.aab`.
   - Paste the 1.0.0 release notes. Save → Review → **Start rollout to Internal testing**.
   - Add tester emails and share the opt-in URL. Verify on a real device.
5. Promote the same build up the tracks: **Internal → Closed → Open → Production**.
6. **Production → Create release**, promote the AAB, roll out. The first
   production submission triggers a Google review (hours to a few days).

> **Every later upload** needs a higher `versionCode` in
> `composeApp/build.gradle.kts` (§4.0). Google rejects a repeat of `versionCode 1`.

**CI (no Mac needed) — GitHub Actions → TestFlight:** `.github/workflows/ios-testflight.yml` runs
`iosApp/fastlane/Fastfile` (lane `beta`) on a GitHub macOS runner.
1. App Store Connect → Users and Access → Integrations → **Team Keys** → generate a key with
   **Admin** access (needed to create the certificate, App IDs and profiles). Download the `.p8`
   (once only) and note the Key ID and Issuer ID.
2. `pip install cryptography pyjwt requests`, then
   `python iosApp/fastlane/setup_signing.py AuthKey_XXXX.p8 <key id> <issuer id> <backup dir>`.
   It creates an Apple Distribution certificate, backs up the `.p12` + password, and sets the six
   repo secrets (`ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `DIST_CERT_P12_B64`,
   `DIST_CERT_PASSWORD`, `GOOGLE_SERVICE_INFO_PLIST_B64`). Re-run when the certificate expires (1 year).
3. Trigger: push a tag (`git tag ios-1.1.0-1 && git push origin ios-1.1.0-1`) or, once the
   workflow is on `main`, `gh workflow run ios-testflight.yml`.
4. The build number is picked automatically (latest TestFlight build + 1). Bump
   `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig` for a new version.

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

**CI (no Mac needed): GitHub Actions → TestFlight.** `.github/workflows/ios-testflight.yml`
runs `iosApp/fastlane/Fastfile` (lane `beta`) on a GitHub macOS runner.
1. App Store Connect → Users and Access → Integrations → **Team Keys** → generate a key with
   **Admin** access (needed to create the certificate, App IDs and profiles). Download the
   `.p8` (only possible once) and note the Key ID and Issuer ID.
2. `pip install cryptography pyjwt requests`, then
   `python iosApp/fastlane/setup_signing.py AuthKey_XXXX.p8 <key id> <issuer id> <backup dir>`.
   It creates an Apple Distribution certificate, backs up the `.p12` + password, and sets the
   six repo secrets (`ASC_KEY_ID`, `ASC_ISSUER_ID`, `ASC_KEY_P8`, `DIST_CERT_P12_B64`,
   `DIST_CERT_PASSWORD`, `GOOGLE_SERVICE_INFO_PLIST_B64`). Re-run when the certificate
   expires (1 year).
3. Trigger: push a tag (`git tag ios-1.1.0-1 && git push origin ios-1.1.0-1`) or, once the
   workflow is on `main`, `gh workflow run ios-testflight.yml`.
4. The build number is chosen automatically (latest TestFlight build + 1). For a new version,
   bump `MARKETING_VERSION` in `iosApp/Configuration/Config.xcconfig` (app + widget share it).

---

## 4.3 Banner ads (AdMob) — **removed**

The app shipped 1.0.0 with ads disabled, and the AdMob/Google Mobile Ads
integration has since been **fully removed** — not just toggled off: the
`ui/ads/` package, the `play-services-ads` dependency, the `AD_ID` permission,
and the AdMob `APPLICATION_ID` manifest meta-data are all gone from `main`.
Reason: the only ad ids that ever existed in this project were Google's public
**test** ids, and shipping those to production violates AdMob policy.

Play **ads declaration = No**, no advertising ID in Data safety.

**To add ads again in a future release:** there's nothing to "restore" — the
old `BannerAd` composable + platform actuals are gone, so this would be a
fresh AdMob integration (an AdMob account/app/ad-unit, the SDK dependency on
Android, `play-services-ads` in `androidMain`, an `AD_ID` permission +
`APPLICATION_ID` meta-data in the manifest, `MobileAds.initialize(...)` at
app start, an `AdView`-hosting composable, and — for iOS — the
Google-Mobile-Ads-SDK via Swift Package Manager, `GADApplicationIdentifier` +
`SKAdNetworkItems` in `Info.plist`, `GADMobileAds.sharedInstance().start(...)`
in `iOSApp.swift`, and a `GADBannerView`-hosting `UIKitView`). Whichever
release adds it back should also flip Play's ads declaration to **Yes** and
re-do the Data safety form to declare Advertising ID collection.

---

## 4.4 Push notifications (Firebase Cloud Messaging)

Every install subscribes to a single topic, **`new_articles`**, at launch — no
per-device token database to run or maintain. The WordPress site sends one FCM
message per published post; every device that has the app installed hears
about it. Both the Android and iOS client code already exist and no-op safely
until you complete the setup below (matches the pattern used for the release
keystore: missing config → feature silently skipped, not a crash).

**One-time setup:**
1. Create a project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an **Android app** (package name `gr.panoramapolihnitou.app`) → download
   `google-services.json` → place it at `composeApp/google-services.json`
   (git-ignored; the Gradle plugin only applies once this file exists).
3. Add an **iOS app** (bundle id `gr.panoramapolihnitou.app`) → download
   `GoogleService-Info.plist` → drag it into the `iosApp/iosApp` group in Xcode,
   **adding it to the `iosApp` target** (git-ignored; `AppDelegate.swift` only
   configures Firebase once this file is present in the bundle).
4. **APNs key** (iOS push requires this): Apple Developer → Certificates,
   Identifiers & Profiles → Keys → create an **APNs Auth Key** → upload it to
   Firebase Console → Project Settings → Cloud Messaging → Apple app
   configuration.
5. Rebuild both apps. Test by sending a message to the `new_articles` topic
   from Firebase Console → Messaging → New campaign (target: Topic).

**Triggering a send from WordPress on publish:** see
[`wordpress-integration/push-on-publish.php`](./wordpress-integration/push-on-publish.php)
— a dependency-free PHP snippet (raw OpenSSL, no Composer needed) that hooks
`transition_post_status` and calls the FCM HTTP v1 API. Install it via a
[Code Snippets](https://wordpress.org/plugins/code-snippets/) plugin on the
WordPress site (recommended) or the theme's `functions.php`. Needs a Firebase
**service account** key (Project Settings → Service Accounts → Generate new
private key) — the file has full setup instructions in its header comment.

**iOS Home Screen widget:** `iosApp/PanoramaWidget/` shows the latest headline,
refreshed roughly hourly. It fetches directly from the WordPress REST API
(doesn't need the Firebase setup above) — nothing to configure, it just works
once you build/run the `iosApp` scheme (the widget extension is embedded and
built automatically as part of that target).

**Tapping a notification opens the article.** The PHP snippet sends the post id
as `post_id` in the FCM data payload. `MainActivity` (Android) and `AppDelegate`
(iOS) pass it to `DeepLinks`, and the app fetches and opens the article after
the splash.

---

## 4.5 Contributor area (password-gated uploads + YouTube live)

Drawer → **ΣΥΝΕΡΓΑΤΕΣ → Περιοχή συνεργατών**. With a valid access code a
contributor can:
- send **text + photos (camera or library) + voice recordings** to the newsroom,
  choosing **Email** (info@panoramapolihnitou.gr) or **Google Drive**;
- **broadcast live** to the channel's YouTube stream (codes with the `live` role only).

Everything secret lives in a free **Google Apps Script** web app, not in the
app. The codes are checked server-side, and the stream key is only handed to
`live` codes.

**Setup:** follow [`backend/apps-script/README.md`](./backend/apps-script/README.md),
then paste the web-app URL into `ContributorConfig.ENDPOINT_URL`
(`composeApp/src/commonMain/.../data/remote/ContributorApi.kt`) and rebuild.
While that constant is empty, the screen says the feature isn't enabled yet.

| Piece | Android | iOS |
|---|---|---|
| Camera photo | `TakePicture` + FileProvider (`res/xml/file_paths.xml`) | `UIImagePickerController` |
| Library | Photo Picker (no storage permission) | `PHPickerViewController` (no permission) |
| Voice | `MediaRecorder`, AAC 64 kbps mono `.m4a` | `AVAudioRecorder`, same format |
| Live (RTMPS → YouTube) | RootEncoder 2.5.9 (JitPack) | HaishinKit 1.9.9 (SwiftPM) via `iosApp/iosApp/LiveStreamBridge.swift` |

Photos are downscaled to 2048 px JPEG (metadata, including GPS, is stripped).
One submission is capped at 18 MB.

**Why RootEncoder 2.5.9 and not newer:** 2.7+ is compiled with Kotlin 2.2–2.4,
which this project's Kotlin 2.1 can't read, and 2.6.x depends on Ktor 2, which
clashes with the app's Ktor 3. If you upgrade Kotlin, revisit this pin. Ktor is
pinned to 3.1.1 to match what RootEncoder brings in.

**iOS note:** the Swift bridge exists because HaishinKit has no Objective-C API.
HaishinKit is pinned to `1.9.x` (2.x rewrote the API).

---

## 5. Troubleshooting

| Symptom | Fix |
|---|---|
| Gradle sync fails: *"wrapper jar missing"* | Let Android Studio sync once, or run `gradle wrapper --gradle-version 8.13`. |
| Gradle fails: *"requires Gradle 8.13 or newer"* / *compileSdk 36 unsupported* | Old wrapper or old Android Studio. The project needs Gradle 8.13 + AGP 8.11.2 + SDK 36. |
| *"SDK location not found"* | Create `local.properties` with `sdk.dir` (see `local.properties.sample`). |
| App shows the retry screen | Device offline, or the WordPress API is unreachable — check `https://panoramapolihnitou.gr/wp-json/wp/v2/posts` in a browser. |
| Images don't load | Confirm posts have a *featured image*; the app falls back to a placeholder icon otherwise. |
| iOS: *"ComposeApp framework not found"* | Ensure the "Compile Kotlin Framework" build phase ran; on a Mac run `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` once. |
| Release build not signed | Create `keystore.properties` from the sample (§3.2). Without it, `assembleRelease` produces an **unsigned** APK. |
| `adb` not found | Add `%LOCALAPPDATA%\Android\Sdk\platform-tools` to your PATH. |
| No banners anywhere | Expected — ads are disabled in 1.0.0 (§4.3). |
| Emulator screenshots come out black | `adb exec-out screencap` returns black for this app on some emulator GPU paths. Use `adb emu screenrecord screenshot <dir>` instead (that's how the Play screenshots were captured). |

---

## 6. Handy command reference

```powershell
# Android (Windows PowerShell)
.\gradlew.bat :composeApp:installDebug       # run debug on device/emulator
.\gradlew.bat :composeApp:assembleDebug      # debug APK
.\gradlew.bat :composeApp:assembleRelease    # signed release APK
.\gradlew.bat :composeApp:bundleRelease      # signed release AAB (for Play)
.\build-play-bundle.bat                      # AAB + copy to dist\panorama-release.aab
.\build-preview-apk.bat                      # sideloadable APK for testers
.\gradlew.bat clean                          # clean

# Verify a release artifact
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\aapt2.exe" dump badging `
  composeApp\build\outputs\apk\release\composeApp-release.apk   # targetSdk, permissions
& "$env:LOCALAPPDATA\Android\Sdk\build-tools\36.0.0\zipalign.exe" -c -P 16 -v 4 `
  composeApp\build\outputs\apk\release\composeApp-release.apk   # 16 KB page alignment

# iOS (Mac)
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode   # build shared framework
# then Archive & distribute from Xcode
```
