# Session handoff / discussion history

Purpose: continue this project seamlessly on another machine (e.g. a Mac). This file
captures **what was asked, what was decided, what was built, the problems hit and how
they were fixed, and what to do next.** It travels with the repo, so a fresh Claude Code
(or a human) can pick up without the original chat.

> Sibling docs: [README.md](./README.md) (overview) · [RUNNING.md](./RUNNING.md)
> (run/release/publish) · [CHANGES.md](./CHANGES.md) (file-by-file build log).

---

## 1. Original request

Act as a senior mobile developer and create a **Kotlin Multiplatform app for iOS and
Android** from `../panorama.md`, plus:
- detailed instructions to run locally on a **Windows 11** PC,
- a detailed **README**,
- a **CHANGES** file tracking what was built,
- a doc covering **local dev**, **dev releases**, and **publishing to the App Store &
  Google Play**.

Follow-up: *"keep history of our discussion so I can use it from my Mac as well"* → this file.

## 2. The spec (`panorama.md`) in one line

Mobile app for **panoramapolihnitou.gr** (WordPress). Features: home + featured slider,
posts as cards, article list, article detail, categories, static pages, bookmarking,
social sharing. REST base: `https://panoramapolihnitou.gr/wp-json/wp/v2/`.
Chosen stack: **Option C — Kotlin Multiplatform**.

## 3. Decisions taken (confirmed with the user)

| Question | Decision |
|---|---|
| Shared UI approach | **Compose Multiplatform** (one shared Kotlin/Compose UI for both platforms) |
| Article HTML rendering | **Native Compose** — parse WordPress HTML into styled blocks (no WebView) |
| Build target given Windows | **Android buildable now on Windows** + full iOS sources/docs for building later on a Mac |

Other engineering choices: Voyager (nav + screen models), Ktor 3 (OkHttp/Darwin),
kotlinx.serialization, Coil 3 (images), multiplatform-settings (bookmarks), a tiny
manual DI container (`AppGraph`). Versions pinned in `gradle/libs.versions.toml`
(Kotlin 2.1.0, CMP 1.7.3, Ktor 3.0.3, Coil 3.0.4, Voyager 1.1.0-beta03, AGP 8.5.2,
Gradle 8.9).

## 4. What was built

A complete project under `panorama-app/`. Full inventory + reasoning is in
[CHANGES.md](./CHANGES.md). High level:
- **Shared (`composeApp/src/commonMain`)**: data models, WordPress REST client + DTOs +
  mappers, bookmark store, repository, HTML parser + date/share utils, Material 3 theme,
  reusable components (cards, featured slider, HTML renderer, network image, state views),
  bottom-tab navigation, and all screens (home, article list, detail, categories,
  bookmarks, pages).
- **Android (`androidMain`)**: `MainActivity`, `Application`, OkHttp engine, share sheet,
  manifest + resources + adaptive launcher icon.
- **iOS (`iosMain` + `iosApp/`)**: Compose UIViewController entry point, Darwin engine,
  `UIActivityViewController` share, SwiftUI host + `Info.plist` + `.xcodeproj`.
- **Docs**: README, RUNNING, CHANGES, and this HANDOFF.

**Validated against the live API:** the DTOs match real responses — `title.rendered`,
`content.rendered`, `_embedded['wp:featuredmedia'][0].source_url` +
`media_details.sizes` (uses `medium_large`), and `_embedded['wp:term']` (nested arrays).

## 5. Problems hit & fixes (chronological)

1. **`build.gradle.kts` — `Unresolved reference: load`** inside
   `Properties().apply { load(...) }`. Cause: implicit-receiver ambiguity in the Gradle
   Kotlin DSL. → Rewrote to an explicit call.
2. **`Unresolved reference: util`** on `java.util.Properties`. Cause: the KMP/AGP DSL
   receiver chain shadows the `java` package identifier inside the `android { }` block.
   → Added top-of-file imports `import java.util.Properties` /
   `import java.io.FileInputStream` and referenced them **unqualified**
   (`Properties()`, `FileInputStream(...)`). Imports resolve against packages regardless
   of the shadowing.
3. **Same error re-appeared after the fix.** Cause: Gradle had **cached the old compiled
   build script** (the error log still quoted the pre-edit lines). →
   - Set `org.gradle.configuration-cache=false` in `gradle.properties` (it was serving
     the stale script).
   - Deleted the project `.gradle/` folder and `~/.gradle/.tmp`.
   - Recovery ritual: `./gradlew --stop` → re-sync (or Android Studio *Invalidate Caches
     / Restart*).

**Current state of the signing block** (`composeApp/build.gradle.kts` release type): it
only runs if a `keystore.properties` file exists, so it does **not** affect debug builds.
The code is correct; the earlier failures were all script-compilation/caching artefacts.

## 6. Where things stand

- Android build: fixes applied; awaiting a clean re-sync to confirm it compiles/runs.
- iOS: never built here (needs a Mac). All sources present.
- Nothing is committed to git yet (no repo initialised).

## 7. Continuing on the Mac — quick start

1. Get the project onto the Mac (OneDrive sync, or `git init` + push then clone — see §8).
2. Install **Xcode 15+**, **Android Studio** (+ Kotlin Multiplatform plugin), and JDK 17.
   Run `brew install kdoctor && kdoctor` to verify the KMP toolchain.
3. **Android** (sanity check the shared code): open `panorama-app/` in Android Studio,
   sync, run `composeApp` on an emulator.
4. **iOS**: follow [RUNNING.md §2.3](./RUNNING.md#23-ios--on-a-mac) — open
   `iosApp/iosApp.xcodeproj` in Xcode (or use the KMP run config), pick a simulator, Run.
   The "Compile Kotlin Framework" build phase runs
   `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` automatically.
   - If Xcode says the project is damaged, regenerate the iOS app via
     <https://kmp.jetbrains.com/> and drop in `iosApp/iosApp/*.swift` + `Info.plist`
     (the Kotlin interop call `MainViewControllerKt.MainViewController()` is unchanged).
5. Publishing to both stores: [RUNNING.md §4](./RUNNING.md#4-publishing-to-the-stores).

**Gotcha carried over from Windows:** if a Gradle script error looks stale (quotes code
that no longer exists), run `./gradlew --stop`, delete `.gradle/`, and re-sync.

## 8. Recommended: put it in git (best cross-machine history)

This gives you real, portable history instead of relying on OneDrive:

```bash
cd panorama-app
git init
git add .
git commit -m "Initial KMP app for panoramapolihnitou.gr (Android + iOS)"
# then create an empty repo on GitHub/GitLab and:
git remote add origin <your-repo-url>
git push -u origin main
```

`.gitignore` is already set up (build outputs, keystores, local.properties are excluded).
On the Mac: `git clone <your-repo-url>` and continue.

## 9. Open TODOs (from CHANGES.md)

- Replace placeholder launcher icon with production assets (Android Image Asset Studio);
  add an iOS `AppIcon` asset catalog.
- Optional features: pull-to-refresh, search UI (API layer already supports `search`),
  offline caching (SQLDelight), analytics/crash reporting.
- Add tests (repository + `util/Html.kt` parser are the natural first targets).
- Optional: GitHub Actions CI (Android AAB + fastlane lanes for both stores).

---

_Last updated: 2026-07-23 (Windows session). Append new sessions below this line._
