# App Store Connect listing copy — Panorama Πολιχνίτου

Primary language: **Greek (el)**. All fields below are what to paste into
App Store Connect → App Information / Version Information. Character limits
from Apple are noted so nothing gets truncated at submission time.

---

## App name (max 30 chars)

```
Πανόραμα Πολιχνίτου
```
(20 characters — matches `CFBundleDisplayName` in Info.plist exactly.)

## Subtitle (max 30 chars)

```
Ειδήσεις & νέα από τη Λέσβο
```
(28 characters)

## Promotional text (max 170 chars, editable without a new build)

```
Όλη η επικαιρότητα του Πολιχνίτου και της Δυτικής Λέσβου σε μία εφαρμογή:
ειδήσεις, εκδηλώσεις και άρθρα, με ειδοποιήσεις άμεσα στο κινητό σας.
```

## Description (max 4000 chars)

```
Το Πανόραμα Πολιχνίτου φέρνει την επικαιρότητα του Πολιχνίτου και της Δυτικής
Λέσβου στο κινητό σας — απευθείας από το panoramapolihnitou.gr.

ΤΙ ΘΑ ΒΡΕΙΤΕ ΣΤΗΝ ΕΦΑΡΜΟΓΗ
• Καθημερινή ενημέρωση με τα τελευταία άρθρα και ειδήσεις της περιοχής
• Προτεινόμενα/κορυφαία άρθρα σε ένα εναλλασσόμενο πάνελ στην αρχική οθόνη
• Πλοήγηση ανά κατηγορία: Επικαιρότητα, Αθλητισμός, Εκδηλώσεις, Αυτοδιοίκηση,
  Βόρειο Αιγαίο, Ελλάδα/Κόσμος και άλλες
• Αναζήτηση άρθρων με λέξη-κλειδί
• Πλήρες άρθρο με εικόνες, τίτλους και σύνδεσμους, όπως στην ιστοσελίδα
• Αποθήκευση άρθρων (bookmarks) για ανάγνωση offline, αποθηκευμένα στη συσκευή σας
• Κοινοποίηση άρθρων σε φίλους μέσω των εφαρμογών κοινοποίησης του iPhone
• Στοιχεία επικοινωνίας και «Ποιοι Είμαστε» για το Πανόραμα Πολιχνίτου
• Σκούρο θέμα (dark mode) που ακολουθεί τις ρυθμίσεις της συσκευής σας

Η εφαρμογή διαβάζει μόνο δημόσιο περιεχόμενο από το panoramapolihnitou.gr —
δεν απαιτείται λογαριασμός ή σύνδεση.

Ακολουθήστε μας και στο Facebook: facebook.com/panoramapolihnitou
```

## Keywords (max 100 chars, comma-separated, no spaces needed but easier to read with them)

```
Πολιχνίτος,Λέσβος,ειδήσεις,νέα,Δυτική Λέσβος,τοπικά νέα,εφημερίδα,άρθρα,εκδηλώσεις
```

## What's New in This Version (release notes, first submission)

```
Πρώτη έκδοση της εφαρμογής Πανόραμα Πολιχνίτου για iPhone και iPad.
```

## Support URL

```
https://panoramapolihnitou.gr
```
(No dedicated app support page exists yet — if you want a real one, add a
"Support" section to the site, e.g. a contact form or the existing Contact
page. Until then this can point at the ContactScreen equivalent on the web,
https://panoramapolihnitou.gr, or an email link `mailto:info@panoramapolihnitou.gr`.)

## Marketing URL (optional)

```
https://panoramapolihnitou.gr
```

## Privacy Policy URL (required)

```
https://panoramapolihnitou.gr/privacy-policy-3/
```
**Verify before submitting** that this page is an actual privacy policy
(data collection/use disclosure), not just Terms of Use — App Review checks
the content of the linked page, not just that a link resolves.

## Copyright

```
© 2026 Πανόραμα Πολιχνίτου
```

## Category

```
Primary: News
Secondary: (none needed)
```

## Age Rating questionnaire

The app only displays editorial news content pulled from a WordPress site with
no user-generated content, no chat, no gambling, and no unrestricted web
browsing (links open in Safari/SFSafariViewController, not embedded). Answer
"No" to all the mature-content and user-interaction questions. Expected
result: **4+**.

## App Privacy ("Data collected") questionnaire

The app makes no accounts, no analytics SDK, no ad SDK on iOS (AdMob is
Android-only right now — see `BannerAd.ios.kt`), and stores bookmarks only
locally via `NSUserDefaults`. Nothing is sent anywhere except the read-only
HTTPS requests to `panoramapolihnitou.gr`.

Recommended answer: **"Data Not Collected."**

If you later add AdMob to iOS (see `RUNNING.md` §4.3) or any analytics, revisit
this questionnaire — ad SDKs typically require declaring Identifiers
(Advertising ID) and Usage Data, plus an `NSUserTrackingUsageDescription`
string and an App Tracking Transparency prompt.

---

## Screenshots

See `app-previews/screenshots/` — one subfolder per required Apple display
size. Each file is numbered in the order it should appear in App Store
Connect (App Store Connect displays them left-to-right in upload order).
Captured live from the `iPhone 16 Pro Max` simulator (iOS 18.5) at the native
1320×2868 resolution — no scaling applied, ready to upload as-is.

| # | File | Screen | Caption (for reference — captions are not baked into the raw simulator screenshots, add via a screenshot design tool if desired) | Status |
|---|------|--------|---------|--------|
| 1 | `iphone-6.9/01-home.png` | Home / featured slider | Όλη η επικαιρότητα του Πολιχνίτου, σε μία ματιά | ✅ captured |
| 2 | `iphone-6.9/02-article-detail.png` | Article detail | Πλήρες άρθρο με εικόνες και σύνδεσμους | ✅ captured |
| 3 | `iphone-6.9/03-search.png` | Search | Αναζήτηση άρθρων με μία λέξη-κλειδί | ✅ captured |
| — | *(missing)* | Article list (per category) | Τα τελευταία άρθρα, οργανωμένα ανά κατηγορία | ⬜ not captured |
| — | *(missing)* | Categories drawer | Πλοήγηση σε όλες τις κατηγορίες ειδήσεων | ⬜ not captured |
| — | *(missing)* | Contact / Who We Are | Στοιχεία επικοινωνίας και σχετικά με εμάς | ⬜ not captured |

A bonus splash-screen capture is at `screenshots/extras/splash-screen.png` —
not part of the required set (Apple screenshots should show real app
content, not the launch screen) but handy for other marketing use.

**Why the remaining three are missing:** the side drawer (hamburger icon),
the article-list-per-category view, and the static pages are only reachable
through the top app bar / drawer navigation. Automating taps on that specific
top app bar from outside the Simulator was unreliable in this session (the
icons never registered a tap despite many retries), while taps on regular
on-screen content — cards, the search field — worked fine. This looks like an
iOS-Simulator/host-window quirk rather than an app bug. **Fastest way to
finish the set:** in Xcode, Run the `iosApp` scheme on an **iPhone 16 Pro
Max** simulator, then for each remaining screen use **Simulator → Device →
Trigger Screenshot** (⌘S) — it saves straight to the Desktop at the correct
resolution:
1. Tap the ☰ menu icon (top-left) → drawer opens → ⌘S → save as `04-categories-drawer.png`.
2. Tap any category in the drawer → article list loads → ⌘S → save as `05-article-list.png`.
3. Open the drawer again → tap "Επικοινωνία" or "Ποιοι Είμαστε" → ⌘S → save as `06-contact.png`.

Drop the resulting files into `app-previews/screenshots/iphone-6.9/`.

## iPad 13" screenshots — not yet created

Only the iPhone 6.9" set above exists. The **iPad Pro 13-inch (M4/M5)**
simulator is already installed locally; repeat the same 6 screens there
(2064 × 2752 px, portrait) into `app-previews/screenshots/ipad-13/` before
submitting, since this app supports iPad (`TARGETED_DEVICE_FAMILY = "1,2"`).
Apple will *not* auto-generate the iPad set from the iPhone set — it's a
separate required size for a universal app.

## App icon (1024×1024, no alpha)

`app-previews/app-icon/icon-1024.png` — copied from the app's own
`AppIcon.appiconset/icon-1024.png` (already alpha-free, verified with
`sips -g hasAlpha`). This is the App Store Connect **App Information →
App Store icon** upload, separate from the icons baked into the binary.
