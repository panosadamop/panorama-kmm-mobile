/**
 * Πανόραμα Πολιχνίτου — contributor backend (Google Apps Script web app).
 *
 * Free, runs on Google's servers under the newsroom's Google account, and keeps
 * every secret out of the mobile app:
 *   • checks contributor access codes,
 *   • saves submissions (text + photos + voice recordings) to a Drive folder,
 *     or emails them to the newsroom,
 *   • hands the YouTube stream key only to codes with the "live" role.
 *
 * Setup: see README.md next to this file.
 *
 * Script properties (Project Settings → Script properties):
 *   ACCESS_CODES        JSON, e.g.
 *                       {"k7Qe-29fz-Lm4X": {"name": "Μαρία Π.", "roles": ["submit", "live"]},
 *                        "pX3n-8wRa-Tt5c": {"name": "Γιώργος Κ.", "roles": ["submit"]}}
 *   DRIVE_FOLDER_ID     id of the Drive folder that receives submissions
 *   MAIL_TO             optional, default info@panoramapolihnitou.gr
 *   YOUTUBE_STREAM_KEY  the channel's stream key (YouTube Studio → Go live → Stream)
 *   YOUTUBE_INGEST_URL  optional, default rtmps://a.rtmps.youtube.com/live2
 *   YOUTUBE_WATCH_URL   optional, e.g. https://www.youtube.com/@panoramapolihnitou/live
 */

var DEFAULT_MAIL_TO = 'info@panoramapolihnitou.gr';
var DEFAULT_INGEST_URL = 'rtmps://a.rtmps.youtube.com/live2';
var MIN_CODE_LENGTH = 8;
// Gmail's attachment ceiling; the app caps uploads at 18 MB raw so this is a backstop.
var MAX_EMAIL_BYTES = 24 * 1024 * 1024;

function doPost(e) {
  try {
    var req = JSON.parse(e.postData.contents);
    var props = PropertiesService.getScriptProperties();
    var user = authenticate_(req.code, props);
    if (!user) {
      Utilities.sleep(1500); // slows down guessing; codes should be long and random anyway
      return json_({ ok: false, error: 'invalid_code' });
    }

    switch (req.action) {
      case 'verify':
        return json_({ ok: true, name: user.name, roles: user.roles });
      case 'submit':
        if (!hasRole_(user, 'submit')) return json_({ ok: false, error: 'forbidden' });
        return json_(submit_(req, user, props));
      case 'live':
        if (!hasRole_(user, 'live')) return json_({ ok: false, error: 'forbidden' });
        return json_(liveConfig_(props));
      default:
        return json_({ ok: false, error: 'unknown_action' });
    }
  } catch (err) {
    console.error(err);
    return json_({ ok: false, error: 'server_error', message: 'Σφάλμα διακομιστή. Δοκιμάστε ξανά.' });
  }
}

/** Lets you check the deployment from a browser. Reveals nothing. */
function doGet() {
  return json_({ ok: true, service: 'panorama-contributors' });
}

function authenticate_(code, props) {
  if (typeof code !== 'string' || code.length < MIN_CODE_LENGTH) return null;
  var codes = JSON.parse(props.getProperty('ACCESS_CODES') || '{}');
  // hasOwnProperty, not codes[code]: "constructor" or "__proto__" must not unlock anything.
  if (!Object.prototype.hasOwnProperty.call(codes, code)) return null;
  var entry = codes[code];
  if (!entry || typeof entry !== 'object') return null;
  return {
    name: String(entry.name || 'Συνεργάτης'),
    roles: Array.isArray(entry.roles) ? entry.roles.map(String) : []
  };
}

function hasRole_(user, role) {
  return user.roles.indexOf(role) !== -1;
}

function submit_(req, user, props) {
  var title = String(req.title || '').trim() || 'Χωρίς τίτλο';
  var text = String(req.text || '').trim();
  var files = (req.files || []).map(function (f, i) {
    var name = sanitizeFileName_(f.name || ('file-' + (i + 1)));
    return Utilities.newBlob(Utilities.base64Decode(f.data), f.mimeType || 'application/octet-stream', name);
  });
  var stamp = Utilities.formatDate(new Date(), 'Europe/Athens', 'yyyy-MM-dd HH.mm');
  var body =
    'Από: ' + user.name + '\n' +
    'Ημερομηνία: ' + stamp + '\n' +
    'Τίτλος: ' + title + '\n' +
    'Συνημμένα: ' + files.length + '\n\n' +
    (text || '(χωρίς κείμενο)') + '\n';

  if (req.destination === 'drive') {
    var folderId = props.getProperty('DRIVE_FOLDER_ID');
    if (!folderId) return { ok: false, error: 'not_configured' };
    var folder = DriveApp.getFolderById(folderId)
      .createFolder(stamp + ' — ' + user.name + ' — ' + title.substring(0, 60));
    folder.createFile('κείμενο.txt', body, MimeType.PLAIN_TEXT);
    files.forEach(function (blob) { folder.createFile(blob); });
    return { ok: true, message: 'Το υλικό αποθηκεύτηκε στο Google Drive της σύνταξης.' };
  }

  var total = files.reduce(function (sum, b) { return sum + b.getBytes().length; }, 0);
  if (total > MAX_EMAIL_BYTES) return { ok: false, error: 'too_large' };
  var to = props.getProperty('MAIL_TO') || DEFAULT_MAIL_TO;
  MailApp.sendEmail({
    to: to,
    subject: '[Εφαρμογή] ' + title + ' — ' + user.name,
    body: body,
    attachments: files,
    name: 'Εφαρμογή Πανόραμα Πολιχνίτου'
  });
  return { ok: true, message: 'Το υλικό στάλθηκε στο ' + to + '.' };
}

function liveConfig_(props) {
  var key = props.getProperty('YOUTUBE_STREAM_KEY');
  if (!key) return { ok: false, error: 'not_configured' };
  return {
    ok: true,
    rtmpUrl: props.getProperty('YOUTUBE_INGEST_URL') || DEFAULT_INGEST_URL,
    streamKey: key,
    watchUrl: props.getProperty('YOUTUBE_WATCH_URL') || null
  };
}

function sanitizeFileName_(name) {
  return String(name).replace(/[\\/:*?"<>|\u0000-\u001f]/g, '_').substring(0, 120);
}

function json_(obj) {
  return ContentService.createTextOutput(JSON.stringify(obj)).setMimeType(ContentService.MimeType.JSON);
}

/**
 * Run once from the editor (▶ Run → setup). Grants the Drive + Gmail permissions
 * the web app needs, creates the submissions folder and a first access code if
 * they don't exist yet, and prints what is configured. Safe to re-run: it never
 * overwrites existing properties.
 */
function setup() {
  var props = PropertiesService.getScriptProperties();
  MailApp.getRemainingDailyQuota(); // triggers the Gmail permission prompt

  var folderId = props.getProperty('DRIVE_FOLDER_ID');
  if (!folderId) {
    folderId = DriveApp.createFolder('Πανόραμα Πολιχνίτου — Υλικό συνεργατών').getId();
    props.setProperty('DRIVE_FOLDER_ID', folderId);
  }

  if (!props.getProperty('ACCESS_CODES')) {
    var codes = {};
    codes[randomCode_()] = { name: 'Δοκιμή', roles: ['submit', 'live'] };
    props.setProperty('ACCESS_CODES', JSON.stringify(codes));
  }

  var configured = JSON.parse(props.getProperty('ACCESS_CODES'));
  console.log('Drive folder: https://drive.google.com/drive/folders/' + folderId);
  console.log('Mail to: ' + (props.getProperty('MAIL_TO') || DEFAULT_MAIL_TO));
  console.log('YouTube stream key set: ' + (props.getProperty('YOUTUBE_STREAM_KEY') ? 'yes' : 'NO — add YOUTUBE_STREAM_KEY'));
  Object.keys(configured).forEach(function (code) {
    console.log('Access code: ' + code + '  →  ' + configured[code].name + ' ' + JSON.stringify(configured[code].roles));
  });
}

/** 12 random characters as xxxx-xxxx-xxxx (≈62 bits). */
function randomCode_() {
  var hex = Utilities.getUuid().replace(/-/g, '') + Utilities.getUuid().replace(/-/g, '');
  var alphabet = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789';
  var out = '';
  for (var i = 0; i < 12; i++) {
    out += alphabet[parseInt(hex.substr(i * 2, 2), 16) % alphabet.length];
    if (i === 3 || i === 7) out += '-';
  }
  return out;
}
