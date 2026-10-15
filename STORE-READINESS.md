# Store readiness — version 1.1.0 (15 Oct 2026)

Is the app ready to be published and accepted by Google Play and the Apple App Store?

**Short answer: the code is close. Neither store submission is ready until a
handful of setup steps are done. Only a person with the right accounts can do
those steps.** Nothing below needs more feature work.

Legend: ✅ done · 🔧 fixed in this change · ⛔ blocker (must do before submitting) · ⚠️ risk / recommended

---

## 1. Blockers

| # | Store | Item | Why it blocks | What to do |
|---|---|---|---|---|
| 1 | Both | **Privacy policy not public** | Both stores reject a submission without a reachable policy URL that matches the app. The current `/privacy-policy-3/` link is unverified. | Publish `app-previews/play-store/privacy-policy-el.md` (updated for 1.1.0: push, camera/mic, uploads, live) on the site, e.g. `/privacy-policy-app/`. Use that URL in both consoles. |
| 2 | Android | **`google-services.json` missing** | Without it, push notifications silently do nothing on Android. The build succeeds, but the store listing can't honestly promise push. | Firebase console → project `panorama-polihnitou-notificy` → add the Android app `gr.panoramapolihnitou.app` → download → `composeApp/google-services.json`. |
| 3 | iOS | **APNs key not confirmed in Firebase** | Without it, iOS push never arrives. | Apple Developer → Keys → APNs key → upload in Firebase → Cloud Messaging. |
| 4 | Both | **Push has no sender** | Nothing sends a push yet. | Install `wordpress-integration/push-on-publish.php` (now in the repo) and the service-account JSON on the WordPress host. |
| 5 | Both | **Contributor backend not deployed** | The contributor area shows "not enabled" until `ContributorConfig.ENDPOINT_URL` is set. Shipping it like that is allowed, but App Review will flag a dead menu item, so either deploy it or hide the entry. | Follow `backend/apps-script/README.md` (≈15 min), paste the URL, rebuild. |
| 6 | Both | **Reviewer demo code** | Apple Guideline 2.1 and Play "App access" require login-gated features to be reviewable. | Create a review code with both roles and put it in the App Store review notes (`app-previews/app-store-listing.md`) and Play Console → App access. |
| 7 | iOS | **iOS build not verified for this change** | The iOS Kotlin/Swift code was written on Windows and has **not been compiled**. iOS builds need a Mac. | On the Mac: open Xcode, let SwiftPM fetch HaishinKit, build, and run on a **real iPhone** (camera, mic and RTMP don't work in the Simulator). Fix any compile nits. See §4. |

## 2. Store declarations that change with 1.1.0

The answers prepared for 1.0.0 (`play-store-listing.md`: "no data collected")
are **no longer accurate**. Submitting them now would be a policy violation.

**Google Play → Data safety**
- *Device or other IDs*: collected (Firebase installation ID for push), not shared for ads, purpose: App functionality.
- *Photos and videos*, *Audio (voice or sound recordings)*, *Other user-generated content*: collected, **optional**, purpose: App functionality. Only contributors who use the feature.
- Data is encrypted in transit: **Yes**. Deletion requests: via info@panoramapolihnitou.gr.
- *App access*: "All or some functionality is restricted" → demo code.

**App Store Connect → App Privacy** (matches the updated `PrivacyInfo.xcprivacy`)
- Photos or Videos, Audio Data, Other User Content: *not linked to identity, not used for tracking, App Functionality*.
- Device ID (push): Firebase's SDK ships its own privacy manifest. Declare it per Firebase's guidance.

## 3. Checklist by store

### Google Play (Android) — very close
- ✅ Target/compile SDK 36, AGP 8.11.2, 16 KB page alignment (verified for 1.0.0)
- ✅ Signed AAB pipeline (`build-play-bundle.bat`), upload key exists. ⚠️ back it up (RELEASE.md §3.3)
- ✅ No ads SDK, no advertising ID
- 🔧 `versionCode` 1 → **2**, `versionName` **1.1.0** (Play rejects a reused versionCode)
- 🔧 Camera/mic declared as `required="false"`, so phones and tablets without a camera aren't filtered out of the store
- 🔧 Runtime permissions only on use (camera, mic). No storage permission (Photo Picker)
- ✅ Notification permission requested at launch (Android 13+), acceptable to Play
- ⚠️ Rebuild `dist\panorama-release.aab` after the steps above. The one in `dist\` is 1.0.0.
- ⚠️ First RootEncoder release: test live streaming on 2–3 real phones (front/back camera, portrait, weak 4G) before Production.

### Apple App Store (iOS) — more work, higher review risk
- ✅ Privacy manifest, `ITSAppUsesNonExemptEncryption = false`, app icon, widget extension
- 🔧 **`iosApp.entitlements` was missing.** The Xcode project referenced it, but the file wasn't in the repo, so signing/archiving fails on a clean checkout. Added with `aps-environment`.
- 🔧 **`UIRequiredDeviceCapabilities` said `armv7`** (32-bit), which contradicts the arm64-only binary and can trip App Store Connect's device-compatibility validation. Changed to `arm64`.
- 🔧 Camera, microphone and photo-library usage strings (Greek). Without them iOS kills the app the moment the camera opens, and App Review rejects for the missing purpose strings.
- 🔧 Version 1.1.0 (build 3)
- ⚠️ **Guideline 4.2 history**: the first submission was rejected as "just a website". Push + widget + offline already answered that. The contributor tools (camera, audio, live) add much more native weight, but only if the reviewer can reach them, hence blocker #6.
- ⚠️ Guideline 5.1.1: login is not required for the main features, so no account-deletion obligation (codes are issued by the newsroom, not created in-app). ✅
- ⚠️ Live streaming goes to YouTube, not displayed in-app, so it is not in-app UGC under 1.2. Keep it that way, or add moderation/report tools first.

## 4. What was verified in this change

| Check | Result |
|---|---|
| Android `assembleDebug` (JDK 21) | ✅ BUILD SUCCESSFUL |
| Unit tests: Apps Script redirect handling, error mapping, upload payload (`ContributorApiTest`, 5 tests) | ✅ 5/5 pass |
| Backend `Code.gs` against stubbed Google services: bad/short codes, `constructor`/`__proto__` bypass attempts, roles, Drive, email, 25 MB limit, malformed JSON | ✅ 11/11 as expected |
| On emulator (Pixel 10 Pro, API 37): drawer entry → contributor home, voice recording (1:17 → 0.5 MB), camera capture with runtime permission, attachment tiles + size total, send error path, live screen camera preview, LIVE disabled without a stream key | ✅ no crashes |
| Real upload to Drive/email, real YouTube broadcast | ❌ not run. Needs the deployed backend + stream key |
| iOS compile / run | ❌ not run. Needs a Mac (see blocker #7) |
| `push-on-publish.php` | ❌ not run. No PHP here, and it needs the Firebase service account |

## 5. Proposed features to make the app more appealing

Ranked by value for a local news audience versus effort. Each one also
strengthens the App Store "more than a website" case.

| # | Feature | Why readers care | Effort |
|---|---|---|---|
| 1 | **Watch live inside the app.** A "🔴 LIVE" banner on the home screen when the channel is streaming, opening an embedded YouTube player | Turns the new broadcast feature into something every reader sees: village festivals, council meetings, church services, football | S–M (poll the channel's `/live` or YouTube Data API key in Apps Script) |
| 2 | **Notification topics.** Let readers choose categories (Εκδηλώσεις, Αθλητικά, Ανακοινώσεις, Έκτακτα), one FCM topic each | Fewer, more relevant pushes means fewer uninstalls. The WordPress snippet already knows the post's categories. | S |
| 3 | **"Send us news" for everyone.** A public, moderated version of the contributor form (photo + text, no code), rate-limited and landing in a Drive "inbox" | Citizen reporting is the lifeblood of village news. Reuses 90% of what was built. | S (needs a moderation habit + Apple 1.2 report/block wording) |
| 4 | **Events calendar** with "add to calendar" and reminders (panigiria, cultural August, markets) | The most-read category, and it's useful, not just readable | M |
| 5 | **Local services page**: pharmacy on duty (εφημερεύοντα φαρμακεία), ferry/bus times to Mytilene, weather for Polichnitos & Vatera, emergency numbers | Daily reasons to open the app outside of news | M |
| 6 | **Listen to articles** (system text-to-speech, Greek voice) | Older readers and drivers. Native APIs on both platforms. | S |
| 7 | **Photo galleries & "Then and now"**: archive photos of the village swipeable full-screen, with pinch-zoom | Strong emotional pull for the diaspora in Athens, Australia, the US | M |
| 8 | **Home-screen widget for Android** (Glance), matching the iOS one | Parity + visibility | S |
| 9 | **Dark mode** (the theme is light-only today) | Expected by store reviewers and night readers | S |
| 10 | **Comments or reactions** on articles | Engagement, but needs moderation and Apple 1.2 tooling. Do last. | L |

Quick wins to ship alongside 1.1.0: **#2** and **#9**. Highest impact
for the next release: **#1** + **#3**.
