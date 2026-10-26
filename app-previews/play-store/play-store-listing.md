# Google Play listing — Πανόραμα Πολιχνίτου 1.1.0

Everything to paste into Play Console for the **1.1.0** release (versionCode 2):
the **Main store listing**, the **App content** declarations and the release.
Character limits are Google's. The counts in brackets are the actual lengths.

Default language: **Ελληνικά (Greek – Greece, el-GR)**.

Lines marked **[push]** apply only if the build includes
`composeApp/google-services.json` (Firebase). Without it the app has no push
notifications on Android, so leave those lines out.

**What changed since 1.0.0:**
- contributor area (photos, voice recordings, text, live streaming to YouTube);
- camera and microphone permissions;
- push notifications **[push]**;
- new upload key.

The 1.0.0 answers "no data collected" and "no login" are **no longer true**.

---

## App details

| Field | Value |
|---|---|
| App name (max 30) | `Πανόραμα Πολιχνίτου` (19) |
| Package name | `gr.panoramapolihnitou.app` (the test APK is `…app.debug`; never use it in Play) |
| App or game | App |
| Category | **Νέα και περιοδικά** (News & Magazines) |
| Free or paid | Free (no in-app purchases, no ads) |
| Contact email | info@panoramapolihnitou.gr |
| Website | https://panoramapolihnitou.gr |
| Phone | *(optional; leave blank unless you want it public)* |
| Privacy policy | `https://panoramapolihnitou.gr/privacy-policy-app/`. **Must be live first** (see `app-previews/privacy-policy/PUBLISHING.md`) |

---

## Short description (max 80 chars)

```
Ειδήσεις και νέα από τον Πολιχνίτο και τη Δυτική Λέσβο, κάθε μέρα.
```
(66 characters)

## Full description (max 4000 chars)

Remove the line `• Ειδοποιήσεις για νέα άρθρα` if the build has no push **[push]**.

```
Το Πανόραμα Πολιχνίτου φέρνει την επικαιρότητα του Πολιχνίτου και της Δυτικής Λέσβου στο κινητό σας, απευθείας από το panoramapolihnitou.gr.

ΤΙ ΘΑ ΒΡΕΙΤΕ ΣΤΗΝ ΕΦΑΡΜΟΓΗ

• Καθημερινή ενημέρωση με τα τελευταία άρθρα και ειδήσεις της περιοχής
• Προτεινόμενα άρθρα σε ένα εναλλασσόμενο πάνελ στην αρχική οθόνη
• Πλοήγηση ανά κατηγορία: Πολιχνίτος, Επικαιρότητα, Λέσβος, Ελλάδα/Κόσμος, Αθλητισμός, Εκδηλώσεις, Αυτοδιοίκηση και άλλες
• Αναζήτηση άρθρων με λέξη-κλειδί
• Αποθήκευση άρθρων για να τα διαβάσετε αργότερα
• Ειδοποιήσεις για νέα άρθρα
• Κοινοποίηση άρθρων σε φίλους
• Ρύθμιση μεγέθους γραμματοσειράς και σκούρο θέμα

ΓΙΑ ΤΟΥΣ ΣΥΝΕΡΓΑΤΕΣ ΜΑΣ

Οι ανταποκριτές και συνεργάτες του Πανοράματος, με κωδικό από τη σύνταξη, στέλνουν φωτογραφίες, ηχογραφήσεις και κείμενα απευθείας στη σύνταξη και μεταδίδουν ζωντανά στο κανάλι μας στο YouTube.

ΧΩΡΙΣ ΔΙΑΦΗΜΙΣΕΙΣ

Για την ανάγνωση δεν χρειάζεται λογαριασμός και δεν προβάλλονται διαφημίσεις. Τα αποθηκευμένα άρθρα και οι ρυθμίσεις σας μένουν μόνο στη συσκευή σας.

Ακολουθήστε μας και στο Facebook: facebook.com/panoramapolihnitou
```

Every claim was checked against the 1.1.0 build: dark theme and font size
exist, and there's no ads SDK or advertising ID.

## Release notes — 1.1.0 (max 500 chars per language)

Remove the notifications line if the build has no push **[push]**.

```
<el-GR>
• Περιοχή συνεργατών: αποστολή φωτογραφιών, ηχογραφήσεων και κειμένων στη σύνταξη
• Ζωντανή μετάδοση στο YouTube για συνεργάτες
• Ειδοποιήσεις για νέα άρθρα
• Βελτιώσεις στην αρχική οθόνη
</el-GR>
```

---

## Graphic assets

| Asset | Requirement | File |
|---|---|---|
| App icon | 512×512 PNG, 32-bit | `play-icon-512.png` |
| Feature graphic | 1024×500 PNG/JPEG | `feature-graphic-1024x500.png` |
| Phone screenshots | 2–8, 320–3840 px per side | `screenshots/phone/01…05` |
| Tablet screenshots | optional (only if you list tablet support) | — |
| Promo video | optional | — |

Phone screenshots (1280×2600, captured from a signed release build on a Pixel
10 Pro emulator running Android 17; system bars cropped so no device frame
shows). They predate 1.1.0 but show screens that haven't changed. A shot of the
contributor area is optional.

| # | File | Screen |
|---|---|---|
| 1 | `01-home.png` | Αρχική — προτεινόμενα + ΣΗΜΑΝΤΙΚΑ |
| 2 | `02-article.png` | Άρθρο με εικόνα και τίτλο |
| 3 | `03-article-body.png` | Κείμενο άρθρου |
| 4 | `04-categories.png` | Μενού με κατηγορίες |
| 5 | `05-saved.png` | Αποθηκευμένα άρθρα |

> The iOS screenshots under `app-previews/screenshots/` are **not** for this
> listing: they show iOS UI and Apple device sizes.

---

## App content declarations (Policy and programs → App content)

**Privacy policy:** `https://panoramapolihnitou.gr/privacy-policy-app/`

**App access:** **All or some functionality in my app is restricted** → Add instructions:

| Field | Value |
|---|---|
| Name | `Contributor area` |
| Username | *(leave empty)* |
| Password | the password of the **App Review** partner (WordPress → Συνεργάτες; both roles, no end date) |
| Any other information | `Open the menu (☰) → Συνεργάτες → enter the password. It unlocks photo/audio/text submission to the newsroom and live streaming to YouTube. All other features need no login.` |

Delete the App Review partner in WordPress after approval. Create a new one for
each later review.

**Ads:** *No, my app does not contain ads.*

**Content rating (IARC):**
- **Category:** *Reference, News, or Educational*.
- **No** to: violence, sexuality, language, controlled substances, gambling and crude humour.
- *Users interact or exchange content?* **No.** Partner submissions go privately to the newsroom, and nothing user-made is shown in the app.
- *Shares location?* **No.** *Digital purchases?* **No.**
- Expected: **PEGI 3 / Everyone**.

**Target audience and content:** **18 and over** only. *Unintentionally appeals to children?* **No.**

**News apps:** *Is your app a news app?* **Yes**. Publisher *Πανόραμα Πολιχνίτου*,
https://panoramapolihnitou.gr, info@panoramapolihnitou.gr. The app's
«Ποιοι Είμαστε» and «Επικοινωνία» screens carry the same information.

**Advertising ID:** **No.** Verified: the merged manifest has no `AD_ID` permission.

**Government apps:** No. **Financial features:** none. **Health:** none.

### Data safety

| Question | Answer |
|---|---|
| Collects or shares required user data types? | **Yes** |
| All data encrypted in transit? | **Yes** (HTTPS only) |
| Account creation | **My app does not allow users to create an account** (partner codes are issued by the newsroom) |
| Data deletion | By email to **info@panoramapolihnitou.gr** |

Data types to tick:

| Data type | Collected | Shared | Ephemeral | Required/optional | Purpose |
|---|---|---|---|---|---|
| Photos and videos → **Photos** | Yes | No | No | Optional (users choose) | App functionality |
| Audio → **Voice or sound recordings** | Yes | No | No | Optional | App functionality |
| App activity → **Other user-generated content** (partners' text) | Yes | No | No | Optional | App functionality |
| Device or other IDs **[push]** (Firebase installation ID) | Yes | No | No | Required | App functionality |

"Shared: No" is deliberate. Partners send their material to the newsroom's own
server, and a live broadcast to YouTube is a transfer the partner starts
themselves, which Play doesn't count as sharing. Reading articles collects nothing.

---

## Permissions in the build (for reference)

`INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`, `CAMERA`,
`RECORD_AUDIO`, plus `WAKE_LOCK` and `c2dm.RECEIVE` from Firebase Messaging.
Camera and microphone are requested only when a partner uses them, and are
declared `required="false"`, so devices without them still see the app. No
storage or media-read permission: photos come through the system Photo Picker.
None of these needs a special Play declaration form.

---

## Countries and pricing

Free. Suggested availability: **all countries**, or at least Greece, Cyprus,
Germany, the UK, the US and Australia for the diaspora.

---

## Release

| Item | Value |
|---|---|
| Bundle | `dist/panorama-1.1.0-release.aab` (versionCode **2**, versionName **1.1.0**, target API 36, 16 KB-aligned) |
| First track | **Internal testing** → verify on a phone → (closed test, see below) → Production |
| Release name | `2 (1.1.0)` (Play's default) |

**Closed-testing requirement.** Personal developer accounts created after
13 Nov 2023 must run a closed test with **at least 12 testers opted in for 14
consecutive days** before Production access. Organization accounts are exempt.

---

## Play App Signing and the upload key

Keep **Play App Signing** on. Google holds the app signing key, and we upload with
the upload key **`upload-keystore-2026-10.jks`** (alias `panorama`, valid to 2054,
SHA-256 `8A:AA:A0:05:BC:A6:51:FC:97:38:65:0F:68:2F:D0:2E:A0:B2:9D:58:81:7E:9F:7A:DC:21:19:69:4C:19:32:4D`).
Its password is only in `keystore.properties`. Both files are gitignored. **Back
them up offline.**

- **New app in Play:** the first upload registers this key automatically.
- **App already uploaded with the July key** (`release-keystore.jks`, now lost):
  App integrity → App signing → **Request upload key reset** → *I lost my upload
  key* → upload `dist/upload_certificate.pem`. Wait for Google's confirmation
  (1–2 days), then upload.
- **Don't delete and recreate the app.** Package names are reserved forever,
  even after deletion, so `gr.panoramapolihnitou.app` could never be used again.
- Every later upload needs a **higher versionCode** (`composeApp/build.gradle.kts`).
