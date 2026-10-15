# Contributor backend (Google Apps Script)

The app's **Περιοχή συνεργατών** (contributor area) talks to `Code.gs`, deployed
as a Google Apps Script web app. It's free and needs no server. It runs under the
newsroom's Google account and holds every secret: access codes, the Drive folder,
the destination mailbox and the YouTube stream key. The app contains none of them.

| App feature | What the script does |
|---|---|
| Unlock with a code | Checks the code against `ACCESS_CODES`, returns the name + roles |
| Send → Email | `MailApp` emails text + photos + recordings to `MAIL_TO` (default `info@panoramapolihnitou.gr`) |
| Send → Google Drive | Creates `<date> — <name> — <title>/` in `DRIVE_FOLDER_ID` with `κείμενο.txt` + the files |
| Live broadcast | Returns the YouTube ingest URL + stream key, only for codes with the `live` role |

## One-time setup (≈15 minutes)

1. Sign in to the Google account that should **own** the submissions (ideally
   the newsroom's account, not a personal one). Go to <https://script.google.com>
   and choose **New project**. Name it `Panorama contributors`.
2. Replace the contents of `Code.gs` with this folder's `Code.gs` and save.
3. Create a Drive folder for submissions (e.g. *Υλικό συνεργατών*). Open it and
   copy the id from the URL: `drive.google.com/drive/folders/<THIS PART>`.
4. **Project Settings (⚙) → Script properties → Add**:

   | Property | Value |
   |---|---|
   | `ACCESS_CODES` | see below |
   | `DRIVE_FOLDER_ID` | the id from step 3 |
   | `MAIL_TO` | `info@panoramapolihnitou.gr` (optional, this is the default) |
   | `YOUTUBE_STREAM_KEY` | see *Live streaming* below |
   | `YOUTUBE_WATCH_URL` | optional, e.g. `https://www.youtube.com/@…/live` |

   `ACCESS_CODES` is JSON. Give each person their own code, so you can revoke
   one without affecting the others:
   ```json
   {
     "k7Qe-29fz-Lm4X": {"name": "Μαρία Π.",   "roles": ["submit", "live"]},
     "pX3n-8wRa-Tt5c": {"name": "Γιώργος Κ.", "roles": ["submit"]}
   }
   ```
   - `submit` gives access to photo/audio/text submissions; `live` gives access to live streaming.
   - Codes must be **at least 8 characters**. Use random ones, for example from a
     password manager. To revoke a code, delete its line. The device is signed
     out on its next action.
   - If you'd rather have one shared password for everyone, use a single entry.
5. In the editor, pick the function **`authorize`** and press **Run**. Accept the
   Drive + Gmail permission prompts. (Google warns that the app is unverified.
   That's expected for your own script: *Advanced → Go to Panorama contributors*.)
6. **Deploy → New deployment → Web app**:
   - *Execute as*: **Me**
   - *Who has access*: **Anyone** (the code check happens inside the script)
   Copy the **Web app URL** (ends in `/exec`).
7. In the app, paste that URL into `ContributorConfig.ENDPOINT_URL` in
   `composeApp/src/commonMain/kotlin/gr/panoramapolihnitou/app/data/remote/ContributorApi.kt`,
   then rebuild. Until then the contributor screen says the feature isn't enabled.

**Updating the script later:** *Deploy → Manage deployments → ✏ → Version: New
version*. The URL stays the same, so no app update is needed. Changing script
properties (codes, stream key) takes effect immediately, without redeploying.

## Live streaming (YouTube)

1. The channel must have live streaming enabled: YouTube Studio → **Create → Go
   live**. The first activation takes up to 24 hours and requires a verified
   channel (phone verification). Encoder streaming, which is what the app does,
   has **no subscriber minimum**. The 50-subscriber rule only applies to
   YouTube's own mobile app.
2. YouTube Studio → **Go live → Stream** tab → *Stream settings*: under **Stream
   key** choose/create a **reusable (default) key**, and turn on **Auto-start**
   (and Auto-stop). Copy the key into `YOUTUBE_STREAM_KEY`.
3. With Auto-start on, pressing **LIVE** in the app puts the channel on air
   within a few seconds. Title, description and visibility come from the
   stream's settings in YouTube Studio, so set them there before going live.

> The stream key lets anyone broadcast on your channel. It only ever goes to
> codes with the `live` role. If a phone is lost, remove that code and **reset
> the stream key** in YouTube Studio (then update `YOUTUBE_STREAM_KEY`).

## Limits worth knowing

- **Size:** the app caps one submission at 18 MB (≈ a dozen photos + a 10-minute
  recording). Photos are shrunk to 2048 px JPEG on the phone before upload.
- **Email quota:** `MailApp` allows 100 emails/day on a free Gmail account, or
  1,500/day on Google Workspace. The Drive destination has no practical limit.
- **Speed:** a submission takes a few seconds plus upload time. Apps Script
  cold-starts can add 1–3 seconds.
