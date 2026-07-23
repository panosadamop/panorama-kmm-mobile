# Panorama Πολιχνίτου — Mobile App

A **Kotlin Multiplatform + Compose Multiplatform** application for **Android and iOS**
that surfaces the content of [panoramapolihnitou.gr](https://panoramapolihnitou.gr)
through its WordPress REST API.

> One shared Kotlin codebase (UI + logic) targets both platforms. You can build and
> run **Android on Windows today**; iOS builds require a Mac with Xcode.

---

## ✨ Features

| Feature | Where | Notes |
|---|---|---|
| **Home with featured slider** | `HomeScreen` + `FeaturedSlider` | Swipeable hero carousel of sticky/latest posts |
| **Main posts as cards** | `ArticleCard` | Image, category, title, excerpt, date, bookmark toggle |
| **Article list view** | `ArticleListScreen` | Infinite-scroll pagination, per-category |
| **Full article details** | `ArticleDetailScreen` | Native rendering of WordPress HTML (text, headings, lists, quotes, inline images, tappable links) |
| **Categories navigation** | `AppDrawer` | Live category list in the side drawer → filtered article list |
| **Static pages** | `WhoWeAreScreen` / `ContactScreen` / `TermsScreen` | Ποιοι Είμαστε / Επικοινωνία / Όροι Χρήσης |
| **Side menu (drawer)** | `AppDrawer` + `ModalNavigationDrawer` | Dark-navy slide-in drawer (no bottom tabs), matches the reference app |
| **Search** | `SearchScreen` | Full-text article search |
| **Splash screen** | `SplashScreen` | Animated red gradient + logo |
| **Bookmarking** | `BookmarkStore` | Offline, persisted locally (SharedPreferences / NSUserDefaults) |
| **Social sharing** | `Sharer` (per-platform) | Android share sheet / iOS `UIActivityViewController` |
| **Dark mode** | `PanoramaTheme` | Follows system setting |
| **Greek UI** | throughout | Labels/dates localised to Greek |

---

## 🧱 Tech stack

| Concern | Choice |
|---|---|
| Language | Kotlin 2.1 (Multiplatform) |
| UI | Compose Multiplatform 1.7 (shared for Android + iOS) |
| Navigation | Voyager (side navigation drawer + screen models) |
| Networking | Ktor 3 (OkHttp engine on Android, Darwin on iOS) |
| JSON | kotlinx.serialization |
| Images | Coil 3 (Ktor network loader) |
| Local storage | multiplatform-settings (bookmarks) |
| DI | Lightweight manual container (`AppGraph`) |
| Build | Gradle 8.9, AGP 8.5, version catalog |

---

## 🗂️ Project structure

```
panorama-app/
├── composeApp/                      # The shared KMP module (all app code lives here)
│   ├── src/commonMain/kotlin/gr/panoramapolihnitou/app/
│   │   ├── App.kt                   # Root composable + bottom-tab scaffold
│   │   ├── di/AppGraph.kt           # Dependency container
│   │   ├── data/
│   │   │   ├── model/               # Domain models (Article, Category, StaticPage)
│   │   │   ├── remote/              # Ktor client, WordPress API, DTOs, mappers
│   │   │   ├── local/               # BookmarkStore (persistence)
│   │   │   └── repository/          # ContentRepository (single source of truth)
│   │   ├── ui/
│   │   │   ├── theme/               # Colors, typography, MaterialTheme
│   │   │   ├── components/          # ArticleCard, FeaturedSlider, HtmlContent, …
│   │   │   ├── navigation/Tabs.kt   # Bottom-nav tabs
│   │   │   └── screens/             # home / articles / detail / categories / bookmarks / pages
│   │   └── util/                    # HTML parser, date formatter, Sharer interface
│   ├── src/androidMain/             # Android entry point + actual implementations
│   │   ├── kotlin/…/MainActivity.kt
│   │   ├── kotlin/…/PanoramaApplication.kt
│   │   ├── kotlin/…/util/AndroidSharer.kt
│   │   ├── kotlin/…/data/remote/HttpClient.android.kt
│   │   ├── AndroidManifest.xml
│   │   └── res/                     # icons, strings, theme, colors
│   └── src/iosMain/                 # iOS entry point + actual implementations
│       ├── kotlin/…/MainViewController.kt
│       ├── kotlin/…/util/IosSharer.kt
│       └── kotlin/…/data/remote/HttpClient.ios.kt
├── iosApp/                          # Xcode project (SwiftUI host for the Compose UI)
│   ├── iosApp/ (iOSApp.swift, ContentView.swift, Info.plist)
│   ├── Configuration/Config.xcconfig
│   └── iosApp.xcodeproj/
├── gradle/libs.versions.toml        # Version catalog (single source of dependency versions)
├── build.gradle.kts / settings.gradle.kts
├── README.md                        # ← you are here
├── CHANGES.md                       # Build log / decisions
└── RUNNING.md                       # Run locally · dev releases · publish to the stores
```

---

## 🔌 Backend

- **CMS:** WordPress
- **REST base:** `https://panoramapolihnitou.gr/wp-json/wp/v2/`
- **Endpoints used:** `posts` (with `_embed`), `categories`, `pages`, `media` (embedded)

No API key is required — the endpoints are public and read-only. The app never
writes to the backend.

---

## 🚀 Getting started (short version)

You need **Android Studio** (Ladybug/Meerkat or newer) with the Android SDK.

1. Open the `panorama-app/` folder in Android Studio and let it sync.
2. Select the **`composeApp`** run configuration and an emulator/device.
3. Press **Run ▶**.

That's it for Android on Windows. For the full walkthrough — including iOS,
signed dev releases, and store publishing — see **[RUNNING.md](./RUNNING.md)**.

---

## 📄 Documentation map

- **[RUNNING.md](./RUNNING.md)** — step-by-step local dev, dev releases, and publishing to Google Play & the App Store.
- **[CHANGES.md](./CHANGES.md)** — everything created while building the project, with the reasoning.
- **[HANDOFF.md](./HANDOFF.md)** — session history: decisions, problems & fixes, and how to continue on another machine (e.g. a Mac).

---

## ⚖️ Notes & limitations

- **iOS cannot be compiled on Windows** — Apple's toolchain requires macOS. All iOS
  code and the Xcode project are included so it builds on a Mac unchanged.
- The included launcher icon is a placeholder vector. Generate production icons with
  Android Studio's *Image Asset Studio* (see RUNNING.md).
- The Gradle **wrapper JAR** is provisioned automatically by Android Studio on first
  sync (or run `gradle wrapper --gradle-version 8.9` if you have Gradle installed).
