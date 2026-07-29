# Google Play listing — Πανόραμα Πολιχνίτου

Everything to paste into **Play Console → Main store listing** and the
**Dashboard → Set up your app** questionnaires for the 1.0.0 release.
Character limits are Google's; the counts in brackets are the actual lengths.

Default language: **Ελληνικά (Greek – Greece, el-GR)**.

---

## App details

| Field | Value |
|---|---|
| App name (max 30) | `Πανόραμα Πολιχνίτου` (19) |
| Package name | `gr.panoramapolihnitou.app` |
| App or game | App |
| Category | **Νέα και περιοδικά** (News & Magazines) |
| Tags | ειδήσεις, τοπικές ειδήσεις, Λέσβος |
| Free or paid | Free (no in-app purchases, no ads in 1.0.0) |
| Contact email | info@panoramapolihnitou.gr |
| Website | https://panoramapolihnitou.gr |
| Phone | *(optional — leave blank unless you want it public)* |

---

## Short description (max 80 chars)

```
Ειδήσεις και νέα από τον Πολιχνίτο και τη Δυτική Λέσβο, κάθε μέρα.
```
(66 characters)

## Full description (max 4000 chars)

```
Το Πανόραμα Πολιχνίτου φέρνει την επικαιρότητα του Πολιχνίτου και της Δυτικής Λέσβου στο κινητό σας — απευθείας από το panoramapolihnitou.gr.

ΤΙ ΘΑ ΒΡΕΙΤΕ ΣΤΗΝ ΕΦΑΡΜΟΓΗ

• Καθημερινή ενημέρωση με τα τελευταία άρθρα και ειδήσεις της περιοχής
• Προτεινόμενα άρθρα σε ένα εναλλασσόμενο πάνελ στην αρχική οθόνη
• Πλοήγηση ανά κατηγορία: Πολιχνίτος, Επικαιρότητα, Λέσβος, Ελλάδα/Κόσμος, Αθλητισμός, Εκδηλώσεις, Αυτοδιοίκηση και άλλες
• Αναζήτηση άρθρων με λέξη-κλειδί
• Πλήρες άρθρο με εικόνες, τίτλους και συνδέσμους, όπως στην ιστοσελίδα
• Αποθήκευση άρθρων για να τα διαβάσετε αργότερα, αποθηκευμένα στη συσκευή σας
• Κοινοποίηση άρθρων σε φίλους μέσω των εφαρμογών του κινητού σας
• Ρύθμιση μεγέθους γραμματοσειράς για πιο άνετη ανάγνωση
• Σκούρο θέμα (dark mode) που ακολουθεί τις ρυθμίσεις της συσκευής σας
• Στοιχεία επικοινωνίας και «Ποιοι Είμαστε» για το Πανόραμα Πολιχνίτου

ΧΩΡΙΣ ΛΟΓΑΡΙΑΣΜΟ, ΧΩΡΙΣ ΔΙΑΦΗΜΙΣΕΙΣ

Η εφαρμογή διαβάζει μόνο δημόσιο περιεχόμενο από το panoramapolihnitou.gr. Δεν
απαιτείται λογαριασμός ή σύνδεση, δεν συλλέγονται προσωπικά δεδομένα και δεν
προβάλλονται διαφημίσεις. Τα αποθηκευμένα άρθρα και οι ρυθμίσεις σας παραμένουν
αποκλειστικά στη συσκευή σας.

Ακολουθήστε μας και στο Facebook: facebook.com/panoramapolihnitou
```
(≈1.250 characters — well inside the limit)

## Release notes — 1.0.0 (max 500 chars per language)

```
Η πρώτη έκδοση της εφαρμογής Πανόραμα Πολιχνίτου:
• Τελευταία άρθρα και προτεινόμενες ειδήσεις στην αρχική
• Πλοήγηση ανά κατηγορία και αναζήτηση
• Αποθήκευση άρθρων για ανάγνωση αργότερα
• Κοινοποίηση, ρύθμιση γραμματοσειράς, σκούρο θέμα
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

Phone screenshots (1280×2600, captured from the signed release build on a
Pixel 10 Pro emulator running Android 17; system bars cropped so no device
frame shows):

| # | File | Screen |
|---|---|---|
| 1 | `01-home.png` | Αρχική — προτεινόμενα + ΣΗΜΑΝΤΙΚΑ |
| 2 | `02-article.png` | Άρθρο με εικόνα και τίτλο |
| 3 | `03-article-body.png` | Κείμενο άρθρου |
| 4 | `04-categories.png` | Μενού με κατηγορίες |
| 5 | `05-saved.png` | Αποθηκευμένα άρθρα |

> The iOS screenshots under `app-previews/screenshots/` are **not** for this
> listing — they show iOS UI and Apple device sizes.

---

## Questionnaires (Dashboard → Set up your app)

**App access** — All functionality is available without restrictions. No login,
no special access instructions needed.

**Ads** — *No*, the app contains no ads. (The Google Mobile Ads SDK is not
linked in 1.0.0. If you enable AdMob later you must change this answer, update
Data safety, and re-declare the advertising ID.)

**Content rating** — Answer the IARC questionnaire honestly:
- Category: **News / Information**
- No violence, sexuality, profanity, gambling, or drug references in the app itself
- The app displays editorial news content published by Πανόραμα Πολιχνίτου
- No user-generated content, no chat, no unrestricted web browsing (links open
  in the phone's browser)
- Expected outcome: **PEGI 3 / Everyone**, possibly 12+ if you declare that news
  content may reference real-world events

**Target audience and content** — Target age group **18 and over** (a general
news app is not directed at children). Answer "No" to "appeals to children".

**Data safety** — For 1.0.0 the honest answers are:
- Does your app collect or share any of the required user data types? **No**
- Is all user data encrypted in transit? **Yes** (all requests are HTTPS)
- Do you provide a way for users to request data deletion? Not applicable — no
  data is collected. Saved articles and settings live only on the device and are
  removed when the app is uninstalled.
- Data collected: **none**. Data shared: **none**.

**Government apps** — No. **Financial features** — None.
**Health** — Not a health app.

**News apps declaration** — Play asks news apps to identify the publisher.
Publisher: *Πανόραμα Πολιχνίτου*, https://panoramapolihnitou.gr,
info@panoramapolihnitou.gr. The app's own «Ποιοι Είμαστε» and «Επικοινωνία»
screens carry the same information.

**Privacy policy URL** — **Required before you can publish.** Publish the draft
in `privacy-policy-el.md` on the website (e.g.
`https://panoramapolihnitou.gr/privacy-policy-app/`) and paste that URL into
Play Console → Store settings → Privacy policy.

---

## Countries and pricing

Free app. Suggested availability: **Ελλάδα** plus worldwide (the audience is
mostly the Greek diaspora, so worldwide is usually the better choice).

---

## Play App Signing

Keep **Play App Signing** enabled (the default). You upload with the upload key
in `release-keystore.jks`; Google holds the app signing key. Back up
`release-keystore.jks` and the password from `keystore.properties` offline —
without them you cannot upload future updates with the same upload key.
