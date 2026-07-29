# RELEASE — Google Play 1.0.0 (Android)

Full record of the release-preparation session of **28 July 2026**: what was
decided, what changed, what was verified (with the actual evidence), what shipped,
and what still needs a human. Companion docs: `RUNNING.md` (how to build and
publish), `CHANGES.md` (chronological build log),
`app-previews/play-store/play-store-listing.md` (listing copy + questionnaires).

**Goal given:** publish the app to Google Play, doing whatever that requires.

---

## 1. Starting state

| Item | Was | Problem for Play |
|---|---|---|
| `compileSdk` / `targetSdk` | 34 | Play requires **API 36** for new apps and updates from 31 Aug 2026 |
| AGP / Gradle | 8.5.2 / 8.9 | Neither supports `compileSdk 36` |
| AdMob | Google's public **test** app id + test banner unit, ads enabled | Shipping test ids to production violates AdMob policy and shows fake banners to real users |
| Signing | `keystore.properties.sample` only — no keystore | `assembleRelease` produces an **unsigned** artifact; Play needs a signed AAB |
| Store assets | iOS-only (`app-previews/screenshots/iphone-6.9/`, App Store listing copy) | Wrong device sizes, iOS UI, iPhone-specific copy |
| Privacy policy | none | Play blocks submission without a public URL |

Everything else was in good shape: `versionCode 1` / `versionName 1.0.0`,
`MainActivity` already calls `enableEdgeToEdge()` and the UI handles status-bar
insets, Ktor logging already `LogLevel.NONE`, launcher icons and adaptive icon
present, `.gitignore` already excluded `*.jks`, `keystore.properties`, `*.aab`, `*.apk`.

## 2. Two decisions that needed the owner

Asked before changing anything, because neither could be resolved from the code:

1. **AdMob** → *Disable ads for 1.0.0.* Chosen over supplying real ids or leaving
   the SDK linked. Cleanest submission: no advertising-ID declaration in Data
   safety, no policy exposure. Ads can return in 1.0.1.
2. **Signing key** → *Generate a keystore with a strong random password.*
   Alternative options were supplying a password or reusing an existing key.

## 3. What changed

### 3.1 Target API 36 and the toolchain it forced

`gradle/libs.versions.toml`
- `android-compileSdk` 34 → **36**, `android-targetSdk` 34 → **36** (comment records the Play deadline)
- `agp` 8.5.2 → **8.11.2** (needed for `compileSdk 36`)

`gradle/wrapper/gradle-wrapper.properties`
- Gradle 8.9 → **8.13** (AGP 8.11.2 minimum)

Kotlin 2.1.0, Compose Multiplatform 1.7.3 and every library version were left
alone — the smaller change was tried first and it built, so there was no reason
to also move Kotlin/Compose and risk source-level breakage. `minSdk` stays 24.

### 3.2 AdMob unlinked

- `composeApp/build.gradle.kts` — removed `implementation(libs.play.services.ads)` from `androidMain`
- `composeApp/src/androidMain/AndroidManifest.xml` — removed the `com.google.android.gms.permission.AD_ID` permission, the `com.google.android.gms.ads.APPLICATION_ID` meta-data (which held the test app id `ca-app-pub-3940256099942544~3347511713`), and the now-dead `android:name=".PanoramaApplication"`
- **Deleted** `composeApp/src/androidMain/kotlin/gr/panoramapolihnitou/app/PanoramaApplication.kt` — its only job was `MobileAds.initialize`. Safe: `multiplatform-settings-no-arg` obtains its context via a ContentProvider, not the `Application` subclass (confirmed by the app running afterwards)
- `BannerAd.android.kt` — now a no-op `actual`, matching the iOS stub, with a 6-step KDoc for restoring the `AdView`
- `BannerAd.kt` — `AdConfig.adsEnabled = false`, `bannerAdUnitId = ""` (test unit id removed)

Kept deliberately: the `BannerAd` call sites throughout the UI and the
`play-services-ads` entry in the version catalog, **bumped 23.6.0 → 24.9.0** so a
future restore picks up a 16 KB-page-size-compliant SDK rather than reintroducing
a non-compliant one.

### 3.3 Upload keystore

- Created `release-keystore.jks` — PKCS12, RSA 2048, alias `panorama`, validity
  10000 days (expires **2053-12-13**), DN
  `CN=Panorama Polihnitou, OU=Mobile, O=Panorama Polihnitou, L=Polichnitos, ST=Lesvos, C=GR`,
  32-character random password
- Created `keystore.properties` pointing at it — **the password lives only in
  this file**, which is gitignored and deliberately not reproduced in this
  document
- Fixed `keystore.properties.sample`: `storeFile` was `../release-keystore.jks`,
  but `composeApp/build.gradle.kts` resolves it with `rootProject.file(...)`, so
  the `../` pointed outside the project. Now `release-keystore.jks`

> ⚠️ **Back up `release-keystore.jks` and its password offline now.** Without both
> you cannot upload updates signed with the same upload key. Note that
> `CHANGES.md` mentions a `preview-keystore.jks` created on 23 Jul 2026 — it was
> not present in the working tree, so this new key is the only one that exists.

### 3.4 Tooling and docs

- **New** `build-play-bundle.bat` — checks `keystore.properties` exists, resolves a
  JDK (Android Studio jbr → `JAVA_HOME` → `jdk-21.0.11` → `jdk-17`), runs
  `:composeApp:bundleRelease`, copies the result to `dist\panorama-release.aab`
- `RUNNING.md` — §1.1 prerequisites (API 36, AGP 8.11.2, Gradle 8.13, JDK 17/21),
  §3.0 (the preview APK now signs with the real upload key), §3.2 (keystore
  already exists + backup warning), §4.1 (rewritten Play steps referencing the
  prepared assets and answers), §4.3 (rewritten: ads disabled, how to re-enable),
  §5 (two new troubleshooting rows), §6 (new commands incl. artifact verification)
- `CHANGES.md` — dated entry summarising this work

## 4. Verification

Not assumed — measured on the artifacts and on a device.

**Builds** (JDK 21.0.11, Gradle 8.13, AGP 8.11.2)
- `:composeApp:bundleRelease` — BUILD SUCCESSFUL in 6m 51s
- `:composeApp:assembleRelease` — BUILD SUCCESSFUL in 57s
- `:composeApp:assembleDebug` — BUILD SUCCESSFUL in 2m 58s (debug variant also survives the deleted `Application` class)

**Manifest** (`aapt2 dump badging`)
```
package: name='gr.panoramapolihnitou.app' versionCode='1' versionName='1.0.0'
         compileSdkVersion='36'
targetSdkVersion:'36'
uses-permission: name='android.permission.INTERNET'
uses-permission: name='gr.panoramapolihnitou.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
application-label:'Πανόραμα Πολιχνίτου'
native-code: 'arm64-v8a' 'armeabi-v7a' 'x86' 'x86_64'
```
No `AD_ID`. The `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` entry is a
signature-level permission AndroidX adds automatically — not user-visible.

**No ad code or ids in the shipping artifact**
- `com.google.android.gms.ads` references in `classes*.dex`: **0**
- AdMob resources/entries in the APK: **0**
- occurrences of the test id `ca-app-pub-3940256099942544`: **0**

**16 KB page size** (mandatory for Android 15+ submissions)
- Only native lib is `libandroidx.graphics.path.so` (4 ABIs)
- `llvm-readelf -l` on arm64-v8a and x86_64: every `LOAD` segment aligns to
  `0x4000` = 16384 ✓
- `zipalign -c -P 16 -v 4` on the release APK: all four `.so` entries `OK`,
  "Verification successful" ✓

**Signature** — `jarsigner -verify` reports the bundle signed by a self-signed
certificate expiring 2053-12-13. Self-signed + "certificate chain invalid" is
exactly what an upload key looks like; Play App Signing re-signs for distribution.

**On-device** — release APK installed on a Pixel 10 Pro emulator (Android 17,
API 37, 1280×2856):
- App launches, no `FATAL`/`AndroidRuntime` errors in logcat
- Live content loads from the WordPress API (article "Πολιτιστικός Αύγουστος στο
  Μεσότοπο", 27 Ιουλίου 2026, sections ΣΗΜΑΝΤΙΚΑ / ΕΚΔΗΛΩΣΕΙΣ)
- Edge-to-edge under API 36+ enforcement renders correctly — top bar padded below
  the status bar, no content clipped
- Drawer, category counts, article detail, bookmarking and the saved-articles
  screen all work; no ad slots appear anywhere

### Two dead ends worth recording

- **`adb shell wm size 1080x1920`** (to force a 9:16 capture) made SystemUI ANR on
  this emulator image. Reverted with `wm size reset` + a reboot; don't do it.
- **`adb exec-out screencap -p` returns an all-black PNG for this app** (0.11%
  non-black pixels) while the launcher captures fine (99.96%), and logcat shows
  the app drawing frames normally. An emulator GPU-path artifact, not an app bug
  — confirmed independently by dumping the view hierarchy with `uiautomator`,
  which listed the real article text. Workaround used for every screenshot:
  `adb emu screenrecord screenshot <dir>`. Recorded in RUNNING.md §5.

## 5. Artifacts

| File | Size | Use |
|---|---|---|
| `dist\panorama-release.aab` | 13.4 MB | **Upload this to Play** |
| `dist\panorama-release.apk` | 13.7 MB | Sideload / tester builds |

Both are gitignored. Rebuild either with `build-play-bundle.bat` or
`build-preview-apk.bat`.

## 6. Store assets — `app-previews/play-store/`

| File | Spec |
|---|---|
| `play-icon-512.png` | 512×512, downscaled from `app-previews/app-icon/icon-1024.png` (bicubic) |
| `feature-graphic-1024x500.png` | 1024×500; brand red `#E32B2B` sampled from the icon with a diagonal gradient, the white island/pen mark masked out of the icon, title + tagline set in the app's own `ubuntu_bold.ttf` |
| `screenshots/phone/01-home.png` | Home — featured slider + ΣΗΜΑΝΤΙΚΑ |
| `screenshots/phone/02-article.png` | Article detail |
| `screenshots/phone/03-article-body.png` | Article body text |
| `screenshots/phone/04-categories.png` | Drawer with category counts |
| `screenshots/phone/05-saved.png` | Saved articles (bookmarked first, so not an empty state) |
| `play-store-listing.md` | Every listing field + questionnaire answer |
| `privacy-policy-el.md` | Draft policy to publish on the website |

Screenshots are **1280×2600 (1:2.03)**, captured from the *signed release build*
and cropped by 156 px top / 100 px bottom to drop the status bar, navigation bar
and the emulator's rounded-corner + camera-cutout frame — so they show app UI
only. (A first attempt repainted those artifacts pixel-by-pixel; cropping was
cleaner and left no residue.)

The listing copy is Greek, adapted from the App Store text with the iOS-specific
claims removed — no "iPhone" sharing wording, and **no push-notification claim**,
because the app has no push support and the store listing must not promise it.
The existing `app-previews/screenshots/` (iPhone 6.9") and
`app-store-listing.md` remain for Apple and must **not** be used on Play.

## 7. Still needs a human

1. **Publish the privacy policy** — put `privacy-policy-el.md` on
   panoramapolihnitou.gr (e.g. `/privacy-policy-app/`). This is the hard blocker:
   Play will not accept a submission without the URL.
2. **Back up** `release-keystore.jks` + the password from `keystore.properties`
   (password manager + encrypted offline copy).
3. **Play Console** — create the app (*Πανόραμα Πολιχνίτου*, Greek el-GR,
   News & Magazines, free), keep Play App Signing on, complete the
   questionnaires from `play-store-listing.md` (ads = **No**, Data safety =
   **no data collected**, target audience 18+, news-publisher details), paste the
   listing, upload the assets.
4. **Upload `dist\panorama-release.aab`** to Internal testing → verify on a real
   phone → promote Closed → Open → Production.
5. **Bump `versionCode`** in `composeApp/build.gradle.kts` before every later
   upload; Google rejects a repeated `versionCode 1`.

## 8. Deliberately not done

- **R8/minification** — `isMinifyEnabled` stays `false`. Enabling it would shrink
  the bundle, but reflection-sensitive paths in Ktor/kotlinx-serialization can
  break in ways that only surface on device. It is not a publishing requirement.
  Worth doing as its own change with real device testing, not folded into a release.
- **iOS/App Store work** — out of scope here and needs a Mac; `RUNNING.md` §4.2
  still covers it.
- **Committing** — all changes were left staged/uncommitted for review. 20 files
  changed (+468 / −103) on top of `f4ad027`.
