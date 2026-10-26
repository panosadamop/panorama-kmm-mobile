# Πανόραμα — Συνεργάτες εφαρμογής (WordPress plugin)

Backend of the app's contributor area. From WordPress admin you:

- **add, edit, deactivate and delete partners**: name, optional email,
  password, roles and the **partnership period**: a start and an end date, or
  **Χωρίς λήξη** (no end date);
- **receive their material**: text + photos + voice recordings land in
  **Συνεργάτες → Υλικό συνεργατών** (files in the Media Library), or are emailed
  to the newsroom, as the partner chooses in the app;
- **set the YouTube stream key**, which is only handed to partners with the
  *Ζωντανή μετάδοση* role.

Nothing secret is in the app. It replaces the earlier Google Apps Script backend.

## Install (≈5 minutes)

1. Zip this folder (`panorama-partners/` with the two `.php` files) or use
   `dist/panorama-partners.zip`, then **Plugins → Add New → Upload Plugin** →
   **Activate**.
2. **Συνεργάτες → Ρυθμίσεις**:
   - *Email σύνταξης*: where "Email" submissions go (default: the site admin email).
   - *Κλειδί μετάδοσης*: YouTube Studio → Create → Go live → Stream → Stream key.
     Leave the RTMP address as it is. *Σελίδα παρακολούθησης* is optional.
3. **Συνεργάτες → Νέος συνεργάτης**: name, password (type one, or press
   *Δημιουργία τυχαίου*), roles. **Note the password before saving**: it is
   stored encrypted and can't be shown again. Send it to the partner.
4. Check: open `https://panoramapolihnitou.gr/wp-json/panorama/v1/contributor`
   in a browser. You should see `{"ok":true,"service":"panorama-contributors"}`.
   If you get an error page instead, a security plugin is blocking the REST API
   for visitors. Allow `panorama/v1` in it.

The app (1.1.0 and later) already points at that address. No rebuild is needed when you
add, change or remove partners.

## Day-to-day

| Task | Where |
|---|---|
| New partner / new password | Συνεργάτες → *Νέος συνεργάτης*, or click a name and type a new password |
| Limit a partnership in time | *Περίοδος συνεργασίας*: *Από* / *Έως*, or the 1/3/6/12-month buttons. Both days count in full (Athens time). Empty *Από* means from now |
| Unlimited partnership | Tick *Χωρίς λήξη* |
| Renew an expired partner | Open them, set a new *Έως* (or *Χωρίς λήξη*) and save. The password stays the same |
| Who expires soon | A warning above the list names everyone ending within 7 days. Filters: Ενεργοί / Προγραμματισμένοι / Έληξαν / Ανενεργοί, plus search |
| Lock someone out | *Απενεργοποίηση* (reversible) or *Διαγραφή*. Their device is signed out on its next action |
| See who used the app lately | *Τελευταία χρήση* column |
| Read submitted material | Συνεργάτες → Υλικό συνεργατών. Photos and recordings play inline, with download links |
| App Store / Play reviewer access | Create a partner named e.g. "App Review" with both roles; put its password in the review notes; delete it after approval |

## Security notes

- Passwords are at least 10 characters. Only an HMAC-SHA256 of each is stored,
  keyed with a random secret in the `panorama_partners_secret` option. Two
  partners can't share a password.
- 10 wrong passwords from one IP within 15 minutes lock that IP out for the
  rest of the window (`rate_limited`). Each wrong attempt is also delayed by 1 s.
- Uploads: at most 20 files and 24 MB per submission (the app caps at 18 MB).
  Only real JPEG/PNG images and MP4/M4A audio are accepted, checked by content.
  The stored file name and extension come from that check, never from the client.
- Submissions are private posts. Their files are ordinary Media Library uploads,
  so anyone who knows a file's (random) URL can open it, like any other upload.
- Managing partners and settings needs the *Administrator* role
  (`manage_options`). Reading submissions needs *Editor* (`edit_others_posts`).
- Uninstalling (Plugins → Delete) removes partners, the secret and the settings.
  Received submissions and their files stay.

## Server requirements

WordPress 5.8+, PHP 7.4+. An 18 MB upload was measured to work with PHP's
default `memory_limit` of 128 MB. The settings page warns if it's lower. Some hosts
cap request size at the web server (nginx `client_max_body_size`). The app then
shows "attachments too large", and the host can raise it to ~32 MB.

## API (for the app)

`POST /wp-json/panorama/v1/contributor`, JSON body, always HTTP 200:

| action | extra fields | success |
|---|---|---|
| `verify` | — | `{ok, name, roles[]}` |
| `submit` | `destination` (`wordpress`\|`email`; legacy `drive` = `wordpress`), `title`, `text`, `files[{name, mimeType, data(base64)}]` | `{ok, message}` |
| `live` | — | `{ok, rtmpUrl, streamKey, watchUrl}` |

Outside the partnership period the code is refused with `not_started` or
`expired` and a `message` that includes the date. The app shows it and signs the
partner out. Neither counts towards the wrong-password throttle.

Errors: `{ok:false, error}` with `invalid_code`, `expired`, `not_started`, `forbidden`, `too_large`,
`not_configured`, `rate_limited`, `bad_request`, `server_error` (+ `message`).

## Tested

On WordPress 6.8 / PHP 8.3 (Docker) on 26 Oct 2026, plus `php -l` on PHP 7.4:
- **Partners:** add, edit while keeping the password, deactivate and delete, done through the admin forms. Forged nonces get 403.
- **Requests:** verify, roles, `live` with and without a key, the throttle.
- **Uploads:** JPEG + M4A stored as private submissions with attachments, and an 18 MB upload with 128 MB of PHP memory. Rejected: PHP disguised as a JPEG, invalid base64 and random bytes.
- **Not tested:** real email delivery, because the test box had no mail server.

1.1.0 (partnership periods), on the same setup: in-place upgrade from 1.0.0
with an existing partner (the table gets the new columns and the partner keeps
working, unlimited). Not started / expired / ends today / ends in 3 days /
unlimited partners created through the admin form and checked against the
API. Refused forms: end date before start, and no end date without "Χωρίς λήξη".
Also checked: list counts, filters, search, the expiry warning, and renewing an
expired partner while keeping the password.
