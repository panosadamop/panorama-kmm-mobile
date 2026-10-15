# Publishing the app privacy policy on panoramapolihnitou.gr

**Target URL:** `https://panoramapolihnitou.gr/privacy-policy-app/`

The app's menu item **Πολιτική Απορρήτου** and both store listings point to this
exact address, so use this slug. Keep it separate from the website's own policy
(`/privacy-policy-3/`).

Time needed: about 10 minutes. You need a WordPress account with the
**Editor** or **Administrator** role.

## 1. Before you start: two things to confirm

The policy states two things that must be true in practice:

1. **Retention:** contributor material that isn't published is deleted within
   **12 months**. If you'd rather keep it longer (or shorter), change that
   sentence in §6 of the HTML before publishing.
2. **Contact:** requests go to **info@panoramapolihnitou.gr**, and you must
   answer them within one month.

If the publisher is a registered business or association, you can add its
legal name and address to §1 ("Ποιοι είμαστε"). It's recommended under GDPR,
but not required by the stores. This text is a well-founded template, not legal
advice. Have it checked if you have access to a lawyer.

## 2. Create the page in WordPress

1. Sign in at **https://panoramapolihnitou.gr/wp-admin**.
2. In the left menu, choose **Σελίδες → Προσθήκη νέας** (*Pages → Add New*).
   Use a **Page**, not a Post, so it doesn't appear in the news feed or the app.
3. **Title:** `Πολιτική Απορρήτου Εφαρμογής`
4. Add the content. Which way depends on your editor:
   - **Block editor (Gutenberg)**, the usual case: click **+** → search for
     **"Προσαρμοσμένη HTML"** / **"Custom HTML"** → add it. Open
     `privacy-policy-el.html` in Notepad, select all (**Ctrl+A**), copy, and paste
     into the block. Click **Προεπισκόπηση** (*Preview*) on the block to check
     that headings and bullets look right.
   - **Classic editor:** switch to the **Κείμενο / Text** tab (not *Visual*) and
     paste. Pasting into *Visual* would show the tags as text.
   - **Elementor / theme builder:** use an **HTML** widget and paste into it.
5. **Set the address (slug).** In the right sidebar → **Σελίδα** tab → **URL**
   (or "Μόνιμος σύνδεσμος"), set the slug to `privacy-policy-app`. The full
   address must read `https://panoramapolihnitou.gr/privacy-policy-app/`.
   If WordPress turns it into `privacy-policy-app-2`, a page or trashed page
   with that slug already exists. Delete it permanently
   (Σελίδες → Κάδος) and set the slug again.
6. **Visibility:** in the same sidebar, make sure *Ορατότητα* is **Δημόσια**
   (*Public*), not private or password-protected.
7. Click **Δημοσίευση** (*Publish*) → **Δημοσίευση** again to confirm.

## 3. Check it like a reviewer would

1. Open the URL in a **private/incognito window**, logged out. The page must
   load with the full text, not a login page or a "coming soon"/maintenance
   screen.
2. Open it on a **phone** (mobile data, not Wi-Fi). It must be readable without
   zooming. Reviewers check on phones too.
3. If a cookie banner covers the page, make sure it can be dismissed and the
   text is still readable behind it.
4. If you use a cache plugin (WP Rocket, LiteSpeed Cache, W3 Total Cache, or a
   Cloudflare cache), **purge the cache** after publishing or editing.
5. Optional but good: add the page to the site footer menu
   (Εμφάνιση → Μενού) next to the existing privacy policy.

## 4. Register the URL with the stores and the app

| Where | Field |
|---|---|
| **Google Play Console** → your app → *Policy and programs → App content → Privacy policy* | `https://panoramapolihnitou.gr/privacy-policy-app/` |
| **App Store Connect** → your app → *App Privacy → Privacy Policy URL* | same |
| **The app** | already points there: menu → *Πολιτική Απορρήτου* (opens in the browser) |

Then update the store data-collection answers to match the policy. This is
spelled out in `STORE-READINESS.md` §2. Play's Data safety form and Apple's
App Privacy section must say the same thing the policy says.

## 5. When the policy needs updating later

Edit the same page (don't create a new one, so the URL stays the same), change
the **Τελευταία ενημέρωση** date at the top, and keep
`app-previews/privacy-policy/privacy-policy-el.html` in the repo in sync. Update
it **before** shipping any feature that collects new data, such as ads,
analytics or public submissions.
