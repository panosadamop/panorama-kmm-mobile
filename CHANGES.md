# CHANGES — build log

A chronological record of everything created while building the app, and the reasoning
behind the key decisions. Newest entries at the top.

---

## 2026-07-23 — Typography, tags, per-page menu, category hero, font scaling

- **Ubuntu font** everywhere: bundled `Ubuntu-Light/Regular/Medium/Bold` TTFs (full
  Greek coverage) in `commonMain/composeResources/font/`; `Type.kt` builds an Ubuntu
  `FontFamily` and applies it to the whole Material 3 type scale.
- **Uppercase without accents:** `removeGreekAccents` is now applied before every
  `.uppercase()` (top bar titles, section headers, category labels on cards, slider,
  category hero, article detail) — e.g. "ΆΡΘΡΟ" → "ΑΡΘΡΟ".
- **All tags per post:** `Article.tagNames` added (mapped from embedded `wp:term`
  where `taxonomy == "post_tag"`); the article screen shows every tag as `#chip`s.
- **Hamburger on every page:** `PanoramaTopBar` gained a trailing `actions` slot and
  always shows the menu button; the article detail screen was moved onto it, so it now
  has burger + back + bookmark + share.
- **First article as category hero:** new `FeaturedArticleCard`; `ArticleListScreen`
  renders the first item as a large hero, the rest as normal cards.
- **Adjustable font size, app-wide:** `PreferencesStore` persists a font scale
  (0.9–1.6); `FontSizeControl` (A− / % / A+ dropdown) lives in the top bar on every
  page; `App.kt` applies it by overriding `LocalDensity.fontScale`, so all text on
  home / lists / posts / static pages scales together.

---

## 2026-07-23 — Match the reference app: red theme, side drawer, icons, splash

Aligned the app with the React Native/Expo reference repo
`panosadamop/panoramapolihnitou-mobile` (theme, navigation, assets).

**Theme** — ported `src/theme/colors.js` verbatim into `ui/theme/Colors.kt`
(`PanoramaColors`): deep-red `#B22222` primary, dark-navy `#1A1A2E` drawer, amber
accent. `Theme.kt` is now light-only (the reference is `userInterfaceStyle: light`).
Android `colors.xml`/`themes.xml` updated to the red brand + red splash window.

**Navigation — bottom tabs removed, replaced by a side drawer:**
- Deleted `ui/navigation/Tabs.kt`, `CategoriesScreen`(+model), `PagesScreen`(+model),
  `StaticPageScreen`(+model).
- `App.kt` now shows the animated splash, then hosts the Voyager `Navigator` inside a
  `ModalNavigationDrawer`.
- `ui/navigation/AppDrawer.kt` replicates `DrawerContent.js` exactly: logo header on
  dark-red, main links (Αρχική / Αποθηκευμένα / Αναζήτηση), **ΠΛΗΡΟΦΟΡΙΕΣ** (Ποιοι
  Είμαστε / Επικοινωνία / Όροι Χρήσης), collapsible **ΚΑΤΗΓΟΡΙΕΣ** with dot + name +
  count badge (loaded live), footer "© 2026 Πανόραμα Πολιχνίτου".
- `DrawerController` + `LocalDrawerController` let any screen's header open the drawer.
- `ui/components/PanoramaTopBar.kt`: red header with hamburger + centered ALL-CAPS Greek
  title (accents dropped via `util/Text.kt`) + optional back / search actions.

**New screens:** `search/SearchScreen`(+model), and static `pages/WhoWeAreScreen`,
`ContactScreen`, `TermsScreen` (content ported from the reference screens). Home now
shows the featured slider + **ΣΗΜΑΝΤΙΚΑ** + **ΤΕΛΕΥΤΑΙΑ ΝΕΑ**, using the reference's
featured/important category IDs (repository `getFeatured`/`getImportant`).

**Splash** — `ui/SplashScreen.kt` reproduces `SplashScreen.js`: diagonal red gradient,
logo fade/scale/slide-in, divider, tagline "Η ενημέρωση της Λέσβου", footer; ~2.8s.

**Assets copied from the repo:**
- Android launcher icons: real `mipmap-*` PNGs (hdpi…xxxhdpi) + adaptive `anydpi-v26`
  XMLs (background `#E32B2B`), replacing the placeholder vector.
- Shared `logo.png` + `splash_icon.png` → `commonMain/composeResources/drawable/`
  (Compose resources; `Res` class generated at `gr.panoramapolihnitou.app.resources`).
- iOS `AppIcon.appiconset` → `iosApp/iosApp/Assets.xcassets/` and registered in the
  Xcode project (file ref + resources build phase).

App display name set to "Πανόραμα Πολιχνίτου".

---

## 2026-07-23 — Initial project scaffold (v1.0.0)

Created the complete Kotlin Multiplatform + Compose Multiplatform project from the
`panorama.md` specification. Summary of what was built, grouped by area.

### Decisions made up front

- **Stack: Option C — Kotlin Multiplatform**, with **Compose Multiplatform** for a
  single shared UI across Android + iOS (chosen over "shared logic + native UI" to
  minimise duplicated code).
- **Article rendering: native Compose**, parsing WordPress HTML into styled blocks
  (chosen over an embedded WebView for speed, consistency and a smaller dependency set).
- **Windows-first delivery:** Android is buildable/runnable on the developer's Windows
  PC now; full iOS sources + Xcode project are included to build later on a Mac.
- **Navigation: Voyager** — gives bottom tabs, per-tab back stacks, and lifecycle-aware
  `ScreenModel`s with minimal boilerplate.
- **DI: a tiny manual container** (`AppGraph`) instead of a framework — fewer moving
  parts, trivial to follow.
- **Bookmarks: multiplatform-settings** (SharedPreferences / NSUserDefaults) storing a
  JSON list — simple, reliable, no schema migration overhead for a small dataset.

### Build configuration

- `settings.gradle.kts`, root `build.gradle.kts` — plugin declarations, repositories.
- `gradle/libs.versions.toml` — version catalog (Kotlin 2.1, CMP 1.7.3, Ktor 3.0.3,
  Coil 3.0.4, Voyager 1.1.0-beta03, multiplatform-settings 1.2.0).
- `composeApp/build.gradle.kts` — KMP targets (androidTarget + iosX64/Arm64/SimulatorArm64
  framework `ComposeApp`), all dependencies, Android app config, debug `.debug` suffix,
  release signing wired to an optional `keystore.properties`.
- `gradle.properties` — JVM args, AndroidX, config-cache + build-cache on.
- `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.9), `gradlew` / `gradlew.bat`.
- `proguard-rules.pro` — keep rules for kotlinx.serialization + Ktor.
- `.gitignore`, `local.properties.sample`, `keystore.properties.sample`.

### Shared code (`commonMain`)

**Data layer**
- `data/model/Models.kt` — domain models `Article`, `Category`, `StaticPage`.
- `data/remote/Dtos.kt` — WordPress REST DTOs (posts/categories/pages + `_embedded`
  featured media & terms).
- `data/remote/HttpClientFactory.kt` — `ApiConfig`, lenient `ApiJson`, shared Ktor
  config (content negotiation, logging, timeouts, base URL) + `expect createHttpClient()`.
- `data/remote/WordPressApi.kt` — typed endpoints: posts (paged/filtered/search),
  sticky posts, single post, categories, pages.
- `data/remote/Mappers.kt` — DTO → domain mapping, incl. best-fit featured image size
  and category-name extraction from embedded terms.
- `data/local/BookmarkStore.kt` — persisted bookmark set exposed as a `StateFlow`.
- `data/repository/ContentRepository.kt` — single source of truth with small caches;
  featured falls back from sticky → latest posts.

**Utilities**
- `util/Html.kt` — dependency-free HTML handling: `stripHtml` for excerpts, and
  `parseHtmlToBlocks` that turns post HTML into paragraphs/headings/bullets/quotes/images
  with inline bold/italic and tappable links; HTML entity decoding incl. numeric/hex.
- `util/DateFormat.kt` — ISO date → Greek long date ("1 Μαΐου 2024").
- `util/Sharer.kt` — `Sharer` interface + `LocalSharer` CompositionLocal for native share.

**DI**
- `di/AppGraph.kt` — lazy singletons: HttpClient → WordPressApi → ContentRepository,
  and BookmarkStore.

**UI — theme & components**
- `ui/theme/` — brand colour scheme (light/dark) + reading-friendly typography.
- `ui/components/` — `StateViews` (loading/error/empty), `NetworkImage` (Coil +
  placeholder), `ArticleCard`/`ArticleRow`, `FeaturedSlider` (HorizontalPager + dots),
  `HtmlContent` (renders parsed blocks).

**UI — navigation & screens**
- `ui/navigation/Tabs.kt` — four bottom tabs (Home, Categories, Bookmarks, Pages),
  each hosting its own `Navigator`.
- `App.kt` — root composable: theme + `LocalSharer` + `TabNavigator` + `NavigationBar`.
- `ui/UiState.kt` — shared Loading/Error/Success state.
- Screens + screen models:
  - `home/` — featured slider + latest articles.
  - `articles/` — paginated (infinite-scroll) list, per category.
  - `detail/` — full article: hero image, meta, HTML body, bookmark + share actions.
  - `categories/` — category list → article list.
  - `bookmarks/` — saved articles from local storage (reactive).
  - `pages/` — static pages list + page detail.

### Android (`androidMain`)
- `MainActivity.kt` (edge-to-edge, hosts `App`), `PanoramaApplication.kt`.
- `util/AndroidSharer.kt` — `ACTION_SEND` share chooser.
- `data/remote/HttpClient.android.kt` — OkHttp engine.
- `AndroidManifest.xml`, `res/` (strings, theme, colors, adaptive launcher icon).

### iOS (`iosMain` + `iosApp/`)
- `MainViewController.kt` — `ComposeUIViewController { App(IosSharer()) }`.
- `util/IosSharer.kt` — `UIActivityViewController` share sheet.
- `data/remote/HttpClient.ios.kt` — Darwin engine.
- `iosApp/` — SwiftUI host (`iOSApp.swift`, `ContentView.swift` bridging via
  `MainViewControllerKt`), `Info.plist`, `Configuration/Config.xcconfig`, and an
  `iosApp.xcodeproj` with a "Compile Kotlin Framework" build phase.

### Documentation
- `README.md` — overview, features, architecture, structure, quick start.
- `RUNNING.md` — local dev (Windows/Mac), dev releases, Google Play + App Store publishing.
- `CHANGES.md` — this file.

### Known follow-ups / TODO
- Replace the placeholder launcher icon with production assets (Image Asset Studio) and
  add an iOS `AppIcon` asset catalog.
- Optional: pull-to-refresh, full-text search UI (the API layer already supports search),
  offline article caching (SQLDelight), analytics/crash reporting.
- Add automated tests (repository + HTML parser are the natural first targets).
