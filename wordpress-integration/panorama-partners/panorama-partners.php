<?php
/**
 * Plugin Name:       Πανόραμα — Συνεργάτες εφαρμογής
 * Description:       Κωδικοί πρόσβασης για τους συνεργάτες της εφαρμογής, παραλαβή υλικού (φωτογραφίες, ηχογραφήσεις, κείμενο) και ρυθμίσεις ζωντανής μετάδοσης YouTube.
 * Version:           1.1.0
 * Requires at least: 5.8
 * Requires PHP:      7.4
 * Author:            Πανόραμα Πολιχνίτου
 * License:           GPL-2.0-or-later
 * Text Domain:       panorama-partners
 *
 * Backend of the app's contributor area ("Συνεργάτες"). The app talks to one
 * endpoint, POST /wp-json/panorama/v1/contributor, with a JSON body
 *   {"action": "verify" | "submit" | "live", "code": "<partner password>", ...}
 * and always gets HTTP 200 with {"ok": true, ...} or {"ok": false, "error": "<code>"}.
 * Error codes: invalid_code, expired, not_started, forbidden, too_large,
 * not_configured, rate_limited, bad_request, server_error.
 *
 * Each partnership can be limited to a date range (site timezone, both days
 * inclusive); an empty end date means it never expires.
 *
 * Passwords are never stored: only an HMAC-SHA256 of each one, keyed with a random
 * secret this plugin keeps in the `panorama_partners_secret` option (so rotating
 * the WordPress salts doesn't lock everyone out). Repeated wrong passwords from
 * one IP are throttled.
 */

if (!defined('ABSPATH')) {
    exit;
}

define('PANORAMA_PARTNERS_VERSION', '1.1.0');
define('PANORAMA_PARTNERS_DB_VERSION', 2);
// Partnerships ending within this many days are highlighted in the list.
define('PANORAMA_PARTNERS_EXPIRY_WARNING_DAYS', 7);
define('PANORAMA_PARTNERS_MIN_CODE_LENGTH', 10);
define('PANORAMA_PARTNERS_MAX_FILES', 20);
// The app caps uploads at 18 MB raw; this is the server-side backstop and also
// stays under the usual 25 MB mailbox attachment ceiling.
define('PANORAMA_PARTNERS_MAX_BYTES', 24 * 1024 * 1024);
define('PANORAMA_PARTNERS_MAX_FAILURES', 10);
define('PANORAMA_PARTNERS_FAILURE_WINDOW', 15 * MINUTE_IN_SECONDS);
define('PANORAMA_PARTNERS_DEFAULT_INGEST', 'rtmps://a.rtmps.youtube.com/live2');
define('PANORAMA_PARTNERS_CPT', 'panorama_submission');

/** Roles a partner can hold, as sent to the app. */
function panorama_partners_roles()
{
    return array(
        'submit' => 'Αποστολή υλικού',
        'live'   => 'Ζωντανή μετάδοση',
    );
}

function panorama_partners_table()
{
    global $wpdb;
    return $wpdb->prefix . 'panorama_partners';
}

function panorama_partners_settings()
{
    return wp_parse_args(get_option('panorama_partners_settings', array()), array(
        'mail_to'      => get_option('admin_email'),
        'notify'       => 1,
        'stream_key'   => '',
        'ingest_url'   => PANORAMA_PARTNERS_DEFAULT_INGEST,
        'watch_url'    => '',
    ));
}

// ---------------------------------------------------------------------------
// Install / upgrade
// ---------------------------------------------------------------------------

register_activation_hook(__FILE__, 'panorama_partners_install');
add_action('plugins_loaded', 'panorama_partners_maybe_upgrade');

function panorama_partners_install()
{
    global $wpdb;
    require_once ABSPATH . 'wp-admin/includes/upgrade.php';
    $table = panorama_partners_table();
    $charset = $wpdb->get_charset_collate();
    dbDelta("CREATE TABLE {$table} (
        id bigint(20) unsigned NOT NULL AUTO_INCREMENT,
        name varchar(190) NOT NULL,
        email varchar(190) NOT NULL DEFAULT '',
        code_hash char(64) NOT NULL,
        code_hint varchar(16) NOT NULL DEFAULT '',
        roles varchar(100) NOT NULL DEFAULT '',
        active tinyint(1) NOT NULL DEFAULT 1,
        starts_on date NULL DEFAULT NULL,
        ends_on date NULL DEFAULT NULL,
        created_at datetime NOT NULL,
        last_used_at datetime NULL DEFAULT NULL,
        PRIMARY KEY  (id),
        UNIQUE KEY code_hash (code_hash)
    ) {$charset};");

    if (!get_option('panorama_partners_secret')) {
        add_option('panorama_partners_secret', wp_generate_password(64, true, true), '', 'no');
    }
    update_option('panorama_partners_db_version', PANORAMA_PARTNERS_DB_VERSION);
}

function panorama_partners_maybe_upgrade()
{
    if ((int) get_option('panorama_partners_db_version') !== PANORAMA_PARTNERS_DB_VERSION) {
        panorama_partners_install();
    }
}

// ---------------------------------------------------------------------------
// Partners (data layer)
// ---------------------------------------------------------------------------

function panorama_partners_hash($code)
{
    return hash_hmac('sha256', $code, (string) get_option('panorama_partners_secret'));
}

/** Last 4 characters, so the newsroom can tell codes apart without seeing them. */
function panorama_partners_hint($code)
{
    return '…' . (function_exists('mb_substr') ? mb_substr($code, -4) : substr($code, -4));
}

function panorama_partners_find_by_code($code)
{
    global $wpdb;
    if (!is_string($code) || strlen($code) < PANORAMA_PARTNERS_MIN_CODE_LENGTH) {
        return null;
    }
    $row = $wpdb->get_row($wpdb->prepare(
        'SELECT * FROM ' . panorama_partners_table() . ' WHERE code_hash = %s AND active = 1',
        panorama_partners_hash($code)
    ));
    return $row ?: null;
}

function panorama_partners_get($id)
{
    global $wpdb;
    return $wpdb->get_row($wpdb->prepare('SELECT * FROM ' . panorama_partners_table() . ' WHERE id = %d', $id));
}

function panorama_partners_all()
{
    global $wpdb;
    return $wpdb->get_results('SELECT * FROM ' . panorama_partners_table() . ' ORDER BY active DESC, name ASC');
}

function panorama_partners_role_list($partner)
{
    return array_values(array_intersect(
        array_keys(panorama_partners_roles()),
        array_filter(explode(',', (string) $partner->roles))
    ));
}

/** Today in the site's timezone, as Y-m-d (comparable with the DATE columns). */
function panorama_partners_today()
{
    return wp_date('Y-m-d');
}

/**
 * `inactive` (switched off by hand), `scheduled` (starts later), `expired`
 * (end date passed) or `active`. Dates are inclusive: a partnership ending on
 * the 31st still works all day on the 31st.
 */
function panorama_partners_status($partner)
{
    $today = panorama_partners_today();
    if (!$partner->active) {
        return 'inactive';
    }
    if (!empty($partner->starts_on) && $partner->starts_on > $today) {
        return 'scheduled';
    }
    if (!empty($partner->ends_on) && $partner->ends_on < $today) {
        return 'expired';
    }
    return 'active';
}

function panorama_partners_statuses()
{
    return array(
        'active'    => 'Ενεργοί',
        'scheduled' => 'Προγραμματισμένοι',
        'expired'   => 'Έληξαν',
        'inactive'  => 'Ανενεργοί',
    );
}

function panorama_partners_date($ymd)
{
    return $ymd ? mysql2date('d/m/Y', $ymd) : '';
}

/** Human-readable range, e.g. "01/11/2026 – 31/12/2026" or "από 01/11/2026 · χωρίς λήξη". */
function panorama_partners_period($partner)
{
    $from = panorama_partners_date($partner->starts_on);
    $to = panorama_partners_date($partner->ends_on);
    if ($to === '') {
        return ($from !== '' ? 'από ' . $from . ' · ' : '') . 'χωρίς λήξη';
    }
    return ($from !== '' ? $from : 'έως') . ($from !== '' ? ' – ' : ' ') . $to;
}

/** Days until the end date (0 = ends today), or null when unlimited. */
function panorama_partners_days_left($partner)
{
    if (empty($partner->ends_on)) {
        return null;
    }
    $today = new DateTime(panorama_partners_today());
    $end = new DateTime($partner->ends_on);
    return (int) $today->diff($end)->format('%r%a');
}

/** Accepts the Y-m-d an <input type="date"> sends; anything else is treated as empty. */
function panorama_partners_parse_date($value)
{
    $value = trim((string) $value);
    $date = DateTime::createFromFormat('!Y-m-d', $value);
    return ($date && $date->format('Y-m-d') === $value) ? $value : null;
}

/** A readable random code like `k7Qe-29fz-Lm4X-pX3n` (≈80 bits). */
function panorama_partners_generate_code()
{
    $alphabet = 'abcdefghjkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    $out = '';
    for ($i = 0; $i < 16; $i++) {
        $out .= $alphabet[random_int(0, strlen($alphabet) - 1)];
        if ($i % 4 === 3 && $i < 15) {
            $out .= '-';
        }
    }
    return $out;
}

// ---------------------------------------------------------------------------
// REST endpoint used by the app
// ---------------------------------------------------------------------------

add_action('rest_api_init', function () {
    register_rest_route('panorama/v1', '/contributor', array(
        array(
            'methods'             => 'POST',
            'callback'            => 'panorama_partners_rest',
            'permission_callback' => '__return_true', // authenticated by the partner code in the body
        ),
        array(
            // Lets you check the endpoint from a browser. Reveals nothing.
            'methods'             => 'GET',
            'callback'            => function () {
                return panorama_partners_reply(array('ok' => true, 'service' => 'panorama-contributors'));
            },
            'permission_callback' => '__return_true',
        ),
    ));
});

function panorama_partners_reply(array $data)
{
    $response = new WP_REST_Response($data, 200);
    $response->header('Cache-Control', 'no-store');
    return $response;
}

function panorama_partners_fail($error, $message = null)
{
    $data = array('ok' => false, 'error' => $error);
    if ($message) {
        $data['message'] = $message;
    }
    return panorama_partners_reply($data);
}

function panorama_partners_rest(WP_REST_Request $request)
{
    $req = $request->get_json_params();
    if (!is_array($req)) {
        // Some setups (e.g. enable_post_data_reading=Off, or a SAPI enforcing
        // post_max_size on raw bodies) hand us an empty body when it's too big.
        $length = isset($_SERVER['CONTENT_LENGTH']) ? (int) $_SERVER['CONTENT_LENGTH'] : 0;
        $limit = wp_convert_hr_to_bytes(ini_get('post_max_size'));
        return panorama_partners_fail($limit > 0 && $length > $limit ? 'too_large' : 'bad_request');
    }

    $throttle_key = 'panorama_partners_fail_' . md5(isset($_SERVER['REMOTE_ADDR']) ? $_SERVER['REMOTE_ADDR'] : '');
    $failures = (int) get_transient($throttle_key);
    if ($failures >= PANORAMA_PARTNERS_MAX_FAILURES) {
        return panorama_partners_fail('rate_limited');
    }

    $partner = panorama_partners_find_by_code(isset($req['code']) ? $req['code'] : null);
    if (!$partner) {
        set_transient($throttle_key, $failures + 1, PANORAMA_PARTNERS_FAILURE_WINDOW);
        sleep(1); // slows down guessing
        return panorama_partners_fail('invalid_code');
    }

    // The code is right, so these don't count towards throttling.
    switch (panorama_partners_status($partner)) {
        case 'scheduled':
            return panorama_partners_fail('not_started', sprintf(
                'Η συνεργασία σας ξεκινά στις %s.', panorama_partners_date($partner->starts_on)
            ));
        case 'expired':
            return panorama_partners_fail('expired', sprintf(
                'Η συνεργασία σας έληξε στις %s. Επικοινωνήστε με τη σύνταξη για ανανέωση.', panorama_partners_date($partner->ends_on)
            ));
    }

    global $wpdb;
    $wpdb->update(panorama_partners_table(), array('last_used_at' => current_time('mysql')), array('id' => $partner->id));
    $roles = panorama_partners_role_list($partner);

    try {
        switch (isset($req['action']) ? $req['action'] : '') {
            case 'verify':
                return panorama_partners_reply(array('ok' => true, 'name' => $partner->name, 'roles' => $roles));
            case 'submit':
                if (!in_array('submit', $roles, true)) {
                    return panorama_partners_fail('forbidden');
                }
                return panorama_partners_reply(panorama_partners_submit($req, $partner));
            case 'live':
                if (!in_array('live', $roles, true)) {
                    return panorama_partners_fail('forbidden');
                }
                return panorama_partners_reply(panorama_partners_live_config());
            default:
                return panorama_partners_fail('bad_request');
        }
    } catch (Throwable $e) {
        error_log('[panorama-partners] ' . $e->getMessage());
        return panorama_partners_fail('server_error', 'Σφάλμα διακομιστή. Δοκιμάστε ξανά.');
    }
}

function panorama_partners_live_config()
{
    $settings = panorama_partners_settings();
    if ($settings['stream_key'] === '') {
        return array('ok' => false, 'error' => 'not_configured');
    }
    return array(
        'ok'        => true,
        'rtmpUrl'   => $settings['ingest_url'] ?: PANORAMA_PARTNERS_DEFAULT_INGEST,
        'streamKey' => $settings['stream_key'],
        'watchUrl'  => $settings['watch_url'] ?: null,
    );
}

// ---------------------------------------------------------------------------
// Submissions
// ---------------------------------------------------------------------------

/**
 * Decodes and validates the uploaded files. Only what the app produces is
 * accepted: JPEG/PNG photos (checked with getimagesize) and MP4/M4A audio
 * (checked for the `ftyp` box). The stored extension comes from that check,
 * never from the name the client sent.
 *
 * @return array|string list of ['path', 'name', 'mime'] or an error code
 */
function panorama_partners_decode_files($files, $dir, $partner_id)
{
    if (!is_array($files)) {
        return array();
    }
    if (count($files) > PANORAMA_PARTNERS_MAX_FILES) {
        return 'too_large';
    }
    $out = array();
    $total = 0;
    foreach (array_values($files) as $i => $file) {
        $bytes = base64_decode(isset($file['data']) ? (string) $file['data'] : '', true);
        if ($bytes === false || $bytes === '') {
            return 'bad_request';
        }
        $total += strlen($bytes);
        if ($total > PANORAMA_PARTNERS_MAX_BYTES) {
            return 'too_large';
        }
        $type = panorama_partners_sniff($bytes);
        if (!$type) {
            return 'bad_request';
        }
        // Partner id, not name: sanitize_title() turns Greek into percent-encoded noise.
        $name = sprintf('%s-p%d-%d-%s.%s', gmdate('Ymd-His'), $partner_id, $i + 1, wp_generate_password(6, false), $type['ext']);
        $path = trailingslashit($dir) . $name;
        if (file_put_contents($path, $bytes) === false) {
            throw new RuntimeException('Cannot write ' . $path);
        }
        $out[] = array('path' => $path, 'name' => $name, 'mime' => $type['mime']);
    }
    return $out;
}

function panorama_partners_sniff($bytes)
{
    if (substr($bytes, 4, 4) === 'ftyp') {
        return array('ext' => 'm4a', 'mime' => 'audio/mp4');
    }
    $info = @getimagesizefromstring($bytes);
    if ($info && $info[2] === IMAGETYPE_JPEG) {
        return array('ext' => 'jpg', 'mime' => 'image/jpeg');
    }
    if ($info && $info[2] === IMAGETYPE_PNG) {
        return array('ext' => 'png', 'mime' => 'image/png');
    }
    return null;
}

function panorama_partners_submit($req, $partner)
{
    $title = sanitize_text_field(isset($req['title']) ? $req['title'] : '');
    $title = $title !== '' ? $title : 'Χωρίς τίτλο';
    $text = sanitize_textarea_field(isset($req['text']) ? $req['text'] : '');
    // "drive" is what app builds made for the old Google Apps Script backend send.
    $destination = isset($req['destination']) && in_array($req['destination'], array('wordpress', 'drive'), true)
        ? 'wordpress' : 'email';

    $dir = trailingslashit(get_temp_dir()) . 'panorama-' . wp_generate_password(12, false);
    if (!wp_mkdir_p($dir)) {
        throw new RuntimeException('Cannot create ' . $dir);
    }
    try {
        $files = panorama_partners_decode_files(isset($req['files']) ? $req['files'] : array(), $dir, (int) $partner->id);
        if (is_string($files)) {
            return array('ok' => false, 'error' => $files);
        }
        return $destination === 'wordpress'
            ? panorama_partners_store($title, $text, $files, $partner)
            : panorama_partners_email($title, $text, $files, $partner);
    } finally {
        foreach ((array) glob($dir . '/*') as $leftover) {
            @unlink($leftover);
        }
        @rmdir($dir);
    }
}

function panorama_partners_body($title, $text, $files, $partner)
{
    return 'Από: ' . $partner->name . "\n"
        . 'Ημερομηνία: ' . wp_date('Y-m-d H:i') . "\n"
        . 'Τίτλος: ' . $title . "\n"
        . 'Συνημμένα: ' . count($files) . "\n\n"
        . ($text !== '' ? $text : '(χωρίς κείμενο)') . "\n";
}

function panorama_partners_email($title, $text, $files, $partner)
{
    $settings = panorama_partners_settings();
    $to = $settings['mail_to'];
    $headers = array();
    if (!empty($partner->email) && is_email($partner->email)) {
        $headers[] = 'Reply-To: ' . $partner->name . ' <' . $partner->email . '>';
    }
    $sent = wp_mail(
        $to,
        '[Εφαρμογή] ' . $title . ' — ' . $partner->name,
        panorama_partners_body($title, $text, $files, $partner),
        $headers,
        wp_list_pluck($files, 'path')
    );
    if (!$sent) {
        return array('ok' => false, 'error' => 'server_error', 'message' => 'Η αποστολή email απέτυχε. Δοκιμάστε την επιλογή «Σύνταξη».');
    }
    return array('ok' => true, 'message' => 'Το υλικό στάλθηκε στο ' . $to . '.');
}

function panorama_partners_store($title, $text, $files, $partner)
{
    require_once ABSPATH . 'wp-admin/includes/file.php';
    require_once ABSPATH . 'wp-admin/includes/media.php';
    require_once ABSPATH . 'wp-admin/includes/image.php';

    $post_id = wp_insert_post(array(
        'post_type'    => PANORAMA_PARTNERS_CPT,
        'post_status'  => 'private',
        'post_title'   => $title . ' — ' . $partner->name,
        'post_content' => $text,
    ), true);
    if (is_wp_error($post_id)) {
        throw new RuntimeException($post_id->get_error_message());
    }
    update_post_meta($post_id, '_panorama_partner_id', (int) $partner->id);
    update_post_meta($post_id, '_panorama_partner_name', $partner->name);

    // Our own sniffing already validated these; WordPress's check would reject
    // Android's .m4a, whose contents identify as video/mp4.
    $allow = function ($data, $file, $filename) use ($files) {
        foreach ($files as $f) {
            if ($f['name'] === $filename) {
                return array('ext' => pathinfo($filename, PATHINFO_EXTENSION), 'type' => $f['mime'], 'proper_filename' => false);
            }
        }
        return $data;
    };
    add_filter('wp_check_filetype_and_ext', $allow, 99, 3);
    try {
        foreach ($files as $file) {
            // media_handle_sideload moves the file out of our temp dir.
            $attachment = media_handle_sideload(
                array('name' => $file['name'], 'tmp_name' => $file['path']),
                $post_id,
                $title . ' — ' . $partner->name
            );
            if (is_wp_error($attachment)) {
                throw new RuntimeException($attachment->get_error_message());
            }
        }
    } finally {
        remove_filter('wp_check_filetype_and_ext', $allow, 99);
    }

    $settings = panorama_partners_settings();
    if ($settings['notify'] && is_email($settings['mail_to'])) {
        wp_mail(
            $settings['mail_to'],
            '[Εφαρμογή] Νέο υλικό: ' . $title . ' — ' . $partner->name,
            panorama_partners_body($title, $text, $files, $partner)
                . "\nΔείτε το: " . admin_url('post.php?post=' . $post_id . '&action=edit') . "\n"
        );
    }
    return array('ok' => true, 'message' => 'Το υλικό παραδόθηκε στη σύνταξη.');
}

// ---------------------------------------------------------------------------
// Submissions inbox (admin)
// ---------------------------------------------------------------------------

add_action('init', function () {
    register_post_type(PANORAMA_PARTNERS_CPT, array(
        'labels'          => array(
            'name'          => 'Υλικό συνεργατών',
            'singular_name' => 'Υλικό συνεργάτη',
            'menu_name'     => 'Υλικό συνεργατών',
            'all_items'     => 'Υλικό συνεργατών',
            'edit_item'     => 'Υλικό συνεργάτη',
            'search_items'  => 'Αναζήτηση',
            'not_found'     => 'Δεν υπάρχει υλικό ακόμη.',
        ),
        'public'          => false,
        'show_ui'         => true,
        'show_in_menu'    => 'panorama-partners',
        'show_in_rest'    => false,
        'supports'        => array('title', 'editor'),
        'capability_type' => 'post',
        'capabilities'    => array('create_posts' => 'do_not_allow'), // only the app creates these
        'map_meta_cap'    => true,
    ));
});

add_action('add_meta_boxes_' . PANORAMA_PARTNERS_CPT, function () {
    add_meta_box('panorama-partners-files', 'Συνημμένα', 'panorama_partners_files_box', null, 'normal', 'high');
    add_meta_box('panorama-partners-from', 'Συνεργάτης', function ($post) {
        echo esc_html(get_post_meta($post->ID, '_panorama_partner_name', true));
    }, null, 'side');
});

function panorama_partners_files_box($post)
{
    $attachments = get_children(array('post_parent' => $post->ID, 'post_type' => 'attachment', 'orderby' => 'ID', 'order' => 'ASC'));
    if (!$attachments) {
        echo '<p>Χωρίς συνημμένα.</p>';
        return;
    }
    echo '<div style="display:flex;flex-wrap:wrap;gap:12px">';
    foreach ($attachments as $a) {
        $url = wp_get_attachment_url($a->ID);
        echo '<div style="max-width:240px">';
        if (wp_attachment_is_image($a->ID)) {
            echo '<a href="' . esc_url($url) . '" target="_blank">' . wp_get_attachment_image($a->ID, 'medium') . '</a>';
        } else {
            echo '<audio controls preload="none" src="' . esc_url($url) . '" style="width:240px"></audio>';
        }
        echo '<br><a href="' . esc_url($url) . '" download>Λήψη</a> · <a href="' . esc_url(get_edit_post_link($a->ID)) . '">Πολυμέσα</a></div>';
    }
    echo '</div>';
}

// ---------------------------------------------------------------------------
// Admin: partners + settings
// ---------------------------------------------------------------------------

add_action('admin_menu', function () {
    add_menu_page('Συνεργάτες εφαρμογής', 'Συνεργάτες', 'manage_options', 'panorama-partners', 'panorama_partners_page', 'dashicons-groups', 26);
    add_submenu_page('panorama-partners', 'Συνεργάτες εφαρμογής', 'Συνεργάτες', 'manage_options', 'panorama-partners', 'panorama_partners_page');
    add_submenu_page('panorama-partners', 'Ρυθμίσεις', 'Ρυθμίσεις', 'manage_options', 'panorama-partners-settings', 'panorama_partners_settings_page');
});

add_action('admin_post_panorama_partners_save', 'panorama_partners_handle_save');
add_action('admin_post_panorama_partners_toggle', 'panorama_partners_handle_toggle');
add_action('admin_post_panorama_partners_delete', 'panorama_partners_handle_delete');
add_action('admin_post_panorama_partners_settings', 'panorama_partners_handle_settings');

function panorama_partners_redirect($args)
{
    wp_safe_redirect(add_query_arg($args, admin_url('admin.php?page=panorama-partners')));
    exit;
}

function panorama_partners_handle_save()
{
    if (!current_user_can('manage_options')) {
        wp_die('Δεν επιτρέπεται.');
    }
    check_admin_referer('panorama_partners_save');
    global $wpdb;

    $id = isset($_POST['id']) ? (int) $_POST['id'] : 0;
    $name = sanitize_text_field(wp_unslash(isset($_POST['name']) ? $_POST['name'] : ''));
    $email = sanitize_email(wp_unslash(isset($_POST['email']) ? $_POST['email'] : ''));
    // Not sanitize_text_field: that would silently alter a password containing e.g. "<" or "%20".
    $code = trim((string) wp_unslash(isset($_POST['code']) ? $_POST['code'] : ''));
    $roles = array_intersect(array_keys(panorama_partners_roles()), (array) (isset($_POST['roles']) ? $_POST['roles'] : array()));
    $active = empty($_POST['active']) ? 0 : 1;
    $starts_on = panorama_partners_parse_date(isset($_POST['starts_on']) ? $_POST['starts_on'] : '');
    $ends_on = empty($_POST['unlimited'])
        ? panorama_partners_parse_date(isset($_POST['ends_on']) ? $_POST['ends_on'] : '')
        : null;

    $back = array('edit' => $id ?: 'new');
    if ($name === '') {
        panorama_partners_redirect($back + array('msg' => 'name'));
    }
    if (($id === 0 || $code !== '') && strlen($code) < PANORAMA_PARTNERS_MIN_CODE_LENGTH) {
        panorama_partners_redirect($back + array('msg' => 'short'));
    }

    if (empty($_POST['unlimited']) && $ends_on === null) {
        panorama_partners_redirect($back + array('msg' => 'noend'));
    }
    if ($starts_on !== null && $ends_on !== null && $ends_on < $starts_on) {
        panorama_partners_redirect($back + array('msg' => 'range'));
    }

    $data = array(
        'name'      => $name,
        'email'     => $email,
        'roles'     => implode(',', $roles),
        'active'    => $active,
        'starts_on' => $starts_on, // null → NULL: usable immediately
        'ends_on'   => $ends_on,   // null → NULL: never expires
    );
    if ($code !== '') {
        $data['code_hash'] = panorama_partners_hash($code);
        $data['code_hint'] = panorama_partners_hint($code);
        $clash = $wpdb->get_var($wpdb->prepare(
            'SELECT id FROM ' . panorama_partners_table() . ' WHERE code_hash = %s AND id <> %d',
            $data['code_hash'],
            $id
        ));
        if ($clash) {
            panorama_partners_redirect($back + array('msg' => 'taken'));
        }
    }

    if ($id) {
        $wpdb->update(panorama_partners_table(), $data, array('id' => $id));
    } else {
        $data['created_at'] = current_time('mysql');
        $wpdb->insert(panorama_partners_table(), $data);
    }
    panorama_partners_redirect(array('msg' => 'saved'));
}

function panorama_partners_handle_toggle()
{
    if (!current_user_can('manage_options')) {
        wp_die('Δεν επιτρέπεται.');
    }
    $id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    check_admin_referer('panorama_partners_toggle_' . $id);
    global $wpdb;
    $wpdb->query($wpdb->prepare('UPDATE ' . panorama_partners_table() . ' SET active = 1 - active WHERE id = %d', $id));
    panorama_partners_redirect(array('msg' => 'saved'));
}

function panorama_partners_handle_delete()
{
    if (!current_user_can('manage_options')) {
        wp_die('Δεν επιτρέπεται.');
    }
    $id = isset($_GET['id']) ? (int) $_GET['id'] : 0;
    check_admin_referer('panorama_partners_delete_' . $id);
    global $wpdb;
    $wpdb->delete(panorama_partners_table(), array('id' => $id));
    panorama_partners_redirect(array('msg' => 'deleted'));
}

function panorama_partners_notice()
{
    $messages = array(
        'saved'   => array('success', 'Αποθηκεύτηκε. Ο κωδικός δεν εμφανίζεται ξανά — στείλτε τον στον συνεργάτη τώρα, αν δεν το έχετε κάνει.'),
        'deleted' => array('success', 'Ο συνεργάτης διαγράφηκε. Η συσκευή του αποσυνδέεται στην επόμενη ενέργεια.'),
        'name'    => array('error', 'Συμπληρώστε όνομα.'),
        'short'   => array('error', sprintf('Ο κωδικός πρέπει να έχει τουλάχιστον %d χαρακτήρες.', PANORAMA_PARTNERS_MIN_CODE_LENGTH)),
        'taken'   => array('error', 'Αυτός ο κωδικός χρησιμοποιείται ήδη από άλλον συνεργάτη.'),
        'range'   => array('error', 'Η ημερομηνία λήξης είναι πριν από την ημερομηνία έναρξης.'),
        'noend'   => array('error', 'Ορίστε ημερομηνία λήξης ή επιλέξτε «Χωρίς λήξη».'),
    );
    $key = isset($_GET['msg']) ? sanitize_key($_GET['msg']) : '';
    if (isset($messages[$key])) {
        printf('<div class="notice notice-%s is-dismissible"><p>%s</p></div>', esc_attr($messages[$key][0]), esc_html($messages[$key][1]));
    }
}

function panorama_partners_page()
{
    if (!current_user_can('manage_options')) {
        return;
    }
    $edit = isset($_GET['edit']) ? sanitize_key($_GET['edit']) : '';
    echo '<div class="wrap">';
    if ($edit !== '') {
        panorama_partners_form($edit === 'new' ? null : panorama_partners_get((int) $edit));
    } else {
        panorama_partners_list();
    }
    echo '</div>';
}

function panorama_partners_list()
{
    $roles = panorama_partners_roles();
    $statuses = panorama_partners_statuses();
    $filter = isset($_GET['status']) ? sanitize_key($_GET['status']) : '';
    $search = isset($_GET['s']) ? sanitize_text_field(wp_unslash($_GET['s'])) : '';

    $all = panorama_partners_all();
    $counts = array_fill_keys(array_keys($statuses), 0);
    foreach ($all as $p) {
        $counts[panorama_partners_status($p)]++;
    }
    $lower = function ($text) {
        return function_exists('mb_strtolower') ? mb_strtolower($text) : strtolower($text);
    };
    $partners = array_filter($all, function ($p) use ($filter, $search, $lower) {
        if ($filter !== '' && panorama_partners_status($p) !== $filter) {
            return false;
        }
        return $search === '' || strpos($lower($p->name . ' ' . $p->email), $lower($search)) !== false;
    });

    $base = admin_url('admin.php?page=panorama-partners');
    echo '<h1 class="wp-heading-inline">Συνεργάτες εφαρμογής</h1> ';
    echo '<a class="page-title-action" href="' . esc_url(add_query_arg('edit', 'new', $base)) . '">Νέος συνεργάτης</a>';
    echo '<hr class="wp-header-end">';
    panorama_partners_notice();

    $ending = array_filter($all, function ($p) {
        $left = panorama_partners_days_left($p);
        return panorama_partners_status($p) === 'active' && $left !== null && $left <= PANORAMA_PARTNERS_EXPIRY_WARNING_DAYS;
    });
    if ($ending) {
        $names = array_map(function ($p) {
            return $p->name . ' (' . panorama_partners_date($p->ends_on) . ')';
        }, $ending);
        echo '<div class="notice notice-warning"><p>' . esc_html(sprintf(
            'Λήγουν τις επόμενες %d ημέρες: %s.',
            PANORAMA_PARTNERS_EXPIRY_WARNING_DAYS,
            implode(', ', $names)
        )) . '</p></div>';
    }

    echo '<p>Κάθε συνεργάτης μπαίνει στην εφαρμογή (μενού → Συνεργάτες) με τον κωδικό του, μόνο μέσα στην περίοδο συνεργασίας. Αν απενεργοποιήσετε ή διαγράψετε κάποιον, ή λήξει η περίοδός του, η συσκευή του αποσυνδέεται στην επόμενη ενέργεια.</p>';

    $links = array('<a href="' . esc_url($base) . '"' . ($filter === '' ? ' class="current"' : '') . '>Όλοι <span class="count">(' . count($all) . ')</span></a>');
    foreach ($statuses as $key => $label) {
        $links[] = '<a href="' . esc_url(add_query_arg('status', $key, $base)) . '"' . ($filter === $key ? ' class="current"' : '') . '>'
            . esc_html($label) . ' <span class="count">(' . (int) $counts[$key] . ')</span></a>';
    }
    echo '<ul class="subsubsub"><li>' . implode(' | </li><li>', $links) . '</li></ul>';
    echo '<form method="get" style="float:right;margin:4px 0 8px"><input type="hidden" name="page" value="panorama-partners">'
        . ($filter !== '' ? '<input type="hidden" name="status" value="' . esc_attr($filter) . '">' : '')
        . '<input type="search" name="s" value="' . esc_attr($search) . '" placeholder="Όνομα ή email"> '
        . '<button class="button">Αναζήτηση</button></form><br class="clear">';

    $badge = panorama_partners_badges();
    echo '<table class="widefat striped"><thead><tr><th>Όνομα</th><th>Κωδικός</th><th>Δικαιώματα</th><th>Περίοδος συνεργασίας</th><th>Κατάσταση</th><th>Τελευταία χρήση</th><th></th></tr></thead><tbody>';
    if (!$partners) {
        echo '<tr><td colspan="7">' . ($all ? 'Κανένας συνεργάτης δεν ταιριάζει.' : 'Δεν υπάρχουν συνεργάτες ακόμη.') . '</td></tr>';
    }
    foreach ($partners as $p) {
        $status = panorama_partners_status($p);
        $left = panorama_partners_days_left($p);
        $labels = array_map(function ($r) use ($roles) { return $roles[$r]; }, panorama_partners_role_list($p));
        $edit = add_query_arg('edit', $p->id, $base);
        $toggle = wp_nonce_url(admin_url('admin-post.php?action=panorama_partners_toggle&id=' . $p->id), 'panorama_partners_toggle_' . $p->id);
        $delete = wp_nonce_url(admin_url('admin-post.php?action=panorama_partners_delete&id=' . $p->id), 'panorama_partners_delete_' . $p->id);

        $period = esc_html(panorama_partners_period($p));
        if ($status === 'active' && $left !== null && $left <= PANORAMA_PARTNERS_EXPIRY_WARNING_DAYS) {
            $period .= '<br><span style="color:#d63638">' . esc_html($left === 0 ? 'Λήγει σήμερα' : sprintf('Λήγει σε %d ημ.', $left)) . '</span>';
        }

        echo '<tr>';
        echo '<td><strong><a href="' . esc_url($edit) . '">' . esc_html($p->name) . '</a></strong>'
            . ($p->email ? '<br><span class="description">' . esc_html($p->email) . '</span>' : '') . '</td>';
        echo '<td><code>' . esc_html($p->code_hint) . '</code></td>';
        echo '<td>' . esc_html($labels ? implode(', ', $labels) : '—') . '</td>';
        echo '<td>' . $period . '</td>';
        echo '<td><span style="color:' . esc_attr($badge[$status][0]) . ';font-weight:600">' . esc_html($badge[$status][1]) . '</span></td>';
        echo '<td>' . esc_html($p->last_used_at ? mysql2date('d/m/Y H:i', $p->last_used_at) : '—') . '</td>';
        echo '<td><a href="' . esc_url($edit) . '">Επεξεργασία</a> | <a href="' . esc_url($toggle) . '">' . ($p->active ? 'Απενεργοποίηση' : 'Ενεργοποίηση') . '</a> | '
            . '<a href="' . esc_url($delete) . '" style="color:#b32d2e" onclick="return confirm(\'Διαγραφή του συνεργάτη;\')">Διαγραφή</a></td>';
        echo '</tr>';
    }
    echo '</tbody></table>';
}

/** Colour + label per status, shared by the list and the edit form. */
function panorama_partners_badges()
{
    return array(
        'active'    => array('#00a32a', 'Ενεργός'),
        'scheduled' => array('#2271b1', 'Προγραμματισμένος'),
        'expired'   => array('#787c82', 'Έληξε'),
        'inactive'  => array('#b32d2e', 'Ανενεργός'),
    );
}

function panorama_partners_form($partner)
{
    $is_new = !$partner;
    $current_roles = $is_new ? array('submit') : panorama_partners_role_list($partner);
    $starts_on = $is_new ? panorama_partners_today() : (string) $partner->starts_on;
    $ends_on = $is_new ? '' : (string) $partner->ends_on;
    $unlimited = $ends_on === '';

    echo '<h1>' . ($is_new ? 'Νέος συνεργάτης' : 'Επεξεργασία: ' . esc_html($partner->name)) . '</h1>';
    panorama_partners_notice();
    if (!$is_new) {
        $badge = panorama_partners_badges()[panorama_partners_status($partner)];
        echo '<p>Κατάσταση τώρα: <strong style="color:' . esc_attr($badge[0]) . '">' . esc_html($badge[1]) . '</strong> · '
            . esc_html(panorama_partners_period($partner)) . '</p>';
    }
    echo '<form method="post" action="' . esc_url(admin_url('admin-post.php')) . '">';
    wp_nonce_field('panorama_partners_save');
    echo '<input type="hidden" name="action" value="panorama_partners_save">';
    echo '<input type="hidden" name="id" value="' . ($is_new ? 0 : (int) $partner->id) . '">';
    echo '<table class="form-table" role="presentation">';

    echo '<tr><th><label for="pp-name">Όνομα</label></th><td><input id="pp-name" name="name" class="regular-text" required value="'
        . esc_attr($is_new ? '' : $partner->name) . '"><p class="description">Εμφανίζεται στην εφαρμογή και στο υλικό που στέλνει.</p></td></tr>';

    echo '<tr><th><label for="pp-email">Email</label></th><td><input id="pp-email" name="email" type="email" class="regular-text" value="'
        . esc_attr($is_new ? '' : $partner->email) . '"><p class="description">Προαιρετικό. Χρησιμοποιείται ως «Απάντηση σε» στα email με υλικό.</p></td></tr>';

    echo '<tr><th><label for="pp-code">Κωδικός</label></th><td>'
        . '<input id="pp-code" name="code" class="regular-text code" autocomplete="off" spellcheck="false"' . ($is_new ? ' required' : '')
        . ' minlength="' . PANORAMA_PARTNERS_MIN_CODE_LENGTH . '"> '
        . '<button type="button" class="button" onclick="document.getElementById(\'pp-code\').value=\'' . esc_js(panorama_partners_generate_code()) . '\'">Δημιουργία τυχαίου</button>'
        . '<p class="description">' . sprintf('Τουλάχιστον %d χαρακτήρες. ', PANORAMA_PARTNERS_MIN_CODE_LENGTH)
        . ($is_new ? '' : 'Αφήστε το κενό για να μείνει ο τρέχων (' . esc_html($partner->code_hint) . '). ')
        . 'Αποθηκεύεται κρυπτογραφημένος και δεν εμφανίζεται ξανά, οπότε σημειώστε τον πριν πατήσετε Αποθήκευση.</p></td></tr>';

    echo '<tr><th>Δικαιώματα</th><td>';
    foreach (panorama_partners_roles() as $role => $label) {
        echo '<label style="display:block;margin-bottom:4px"><input type="checkbox" name="roles[]" value="' . esc_attr($role) . '"'
            . checked(in_array($role, $current_roles, true), true, false) . '> ' . esc_html($label) . '</label>';
    }
    echo '</td></tr>';

    echo '<tr><th>Περίοδος συνεργασίας</th><td>'
        . '<label>Από <input type="date" id="pp-start" name="starts_on" value="' . esc_attr($starts_on) . '"></label> '
        . '<label style="margin-left:12px">Έως <input type="date" id="pp-end" name="ends_on" value="' . esc_attr($ends_on) . '"></label>'
        . '<p style="margin:8px 0"><label><input type="checkbox" id="pp-unlimited" name="unlimited" value="1"' . checked($unlimited, true, false)
        . '> <strong>Χωρίς λήξη</strong> (απεριόριστη συνεργασία)</label></p>'
        . '<p>Διάρκεια από την έναρξη: '
        . '<button type="button" class="button button-small" data-months="1">1 μήνας</button> '
        . '<button type="button" class="button button-small" data-months="3">3 μήνες</button> '
        . '<button type="button" class="button button-small" data-months="6">6 μήνες</button> '
        . '<button type="button" class="button button-small" data-months="12">1 έτος</button></p>'
        . '<p class="description">Ο συνεργάτης μπαίνει στην εφαρμογή από την αρχή της πρώτης έως το τέλος της τελευταίας ημέρας (ώρα Ελλάδας). Κενή έναρξη: από τώρα.</p>'
        . '</td></tr>';

    echo '<tr><th>Κατάσταση</th><td><label><input type="checkbox" name="active" value="1"'
        . checked($is_new || $partner->active, true, false) . '> Ενεργός</label>'
        . '<p class="description">Η απενεργοποίηση κλειδώνει τον συνεργάτη αμέσως, ανεξάρτητα από την περίοδο.</p></td></tr>';

    echo '</table>';
    submit_button($is_new ? 'Προσθήκη συνεργάτη' : 'Αποθήκευση');
    echo '</form>';
    // Disabled inputs aren't submitted, which is what "unlimited" wants: the end date is dropped.
    echo "<script>(function () {
        var start = document.getElementById('pp-start'), end = document.getElementById('pp-end'), unlimited = document.getElementById('pp-unlimited');
        function sync() { end.disabled = unlimited.checked; end.required = !unlimited.checked; }
        unlimited.addEventListener('change', sync);
        document.querySelectorAll('[data-months]').forEach(function (button) {
            button.addEventListener('click', function () {
                var from = start.value ? new Date(start.value + 'T00:00:00') : new Date();
                var to = new Date(from.getFullYear(), from.getMonth() + Number(button.dataset.months), from.getDate() - 1);
                end.value = to.getFullYear() + '-' + String(to.getMonth() + 1).padStart(2, '0') + '-' + String(to.getDate()).padStart(2, '0');
                unlimited.checked = false;
                sync();
            });
        });
        sync();
    })();</script>";
}

function panorama_partners_handle_settings()
{
    if (!current_user_can('manage_options')) {
        wp_die('Δεν επιτρέπεται.');
    }
    check_admin_referer('panorama_partners_settings');
    $in = wp_unslash($_POST);
    $current = panorama_partners_settings();
    $stream_key = isset($in['stream_key']) ? trim((string) $in['stream_key']) : '';
    update_option('panorama_partners_settings', array(
        'mail_to'    => sanitize_email(isset($in['mail_to']) ? $in['mail_to'] : ''),
        'notify'     => empty($in['notify']) ? 0 : 1,
        // Blank keeps the saved key, so it never has to be printed back into the page.
        'stream_key' => $stream_key !== '' ? sanitize_text_field($stream_key) : (empty($in['clear_stream_key']) ? $current['stream_key'] : ''),
        'ingest_url' => esc_url_raw(isset($in['ingest_url']) ? $in['ingest_url'] : '', array('rtmp', 'rtmps')),
        'watch_url'  => esc_url_raw(isset($in['watch_url']) ? $in['watch_url'] : ''),
    ), false);
    wp_safe_redirect(admin_url('admin.php?page=panorama-partners-settings&updated=1'));
    exit;
}

function panorama_partners_settings_page()
{
    if (!current_user_can('manage_options')) {
        return;
    }
    $s = panorama_partners_settings();
    echo '<div class="wrap"><h1>Συνεργάτες — Ρυθμίσεις</h1>';
    if (!empty($_GET['updated'])) {
        echo '<div class="notice notice-success is-dismissible"><p>Οι ρυθμίσεις αποθηκεύτηκαν.</p></div>';
    }

    // The app sends JSON, so post_max_size doesn't apply; PHP memory does. An 18 MB
    // upload (the app's cap) was measured to work with the 128 MB default.
    $memory = wp_convert_hr_to_bytes(ini_get('memory_limit'));
    if ($memory > 0 && $memory < 128 * 1024 * 1024) {
        printf(
            '<div class="notice notice-warning"><p>Η μνήμη PHP είναι %s (<code>memory_limit</code>). Για αποστολές έως 18 MB από την εφαρμογή χρειάζονται τουλάχιστον 128 MB — ζητήστε το από τη φιλοξενία. Μικρότερες αποστολές λειτουργούν κανονικά.</p></div>',
            esc_html(size_format($memory))
        );
    }

    echo '<form method="post" action="' . esc_url(admin_url('admin-post.php')) . '">';
    wp_nonce_field('panorama_partners_settings');
    echo '<input type="hidden" name="action" value="panorama_partners_settings">';
    echo '<h2>Υλικό συνεργατών</h2><table class="form-table" role="presentation">';
    echo '<tr><th><label for="pp-mail">Email σύνταξης</label></th><td><input id="pp-mail" name="mail_to" type="email" class="regular-text" value="' . esc_attr($s['mail_to']) . '">'
        . '<p class="description">Εδώ φτάνει το υλικό όταν ο συνεργάτης επιλέγει «Email».</p></td></tr>';
    echo '<tr><th>Ειδοποίηση</th><td><label><input type="checkbox" name="notify" value="1"' . checked($s['notify'], 1, false)
        . '> Στείλε email όταν αποθηκεύεται νέο υλικό στη σύνταξη (Συνεργάτες → Υλικό συνεργατών)</label></td></tr>';
    echo '</table>';

    echo '<h2>Ζωντανή μετάδοση (YouTube)</h2><table class="form-table" role="presentation">';
    echo '<tr><th><label for="pp-key">Κλειδί μετάδοσης</label></th><td><input id="pp-key" name="stream_key" type="password" class="regular-text" autocomplete="new-password" placeholder="'
        . esc_attr($s['stream_key'] !== '' ? 'Έχει οριστεί — αφήστε κενό για να μείνει' : 'YouTube Studio → Δημιουργία → Ζωντανή μετάδοση → Ροή') . '">';
    if ($s['stream_key'] !== '') {
        echo ' <label><input type="checkbox" name="clear_stream_key" value="1"> Αφαίρεση</label>';
    }
    echo '<p class="description">Δίνεται μόνο σε συνεργάτες με δικαίωμα «Ζωντανή μετάδοση».</p></td></tr>';
    echo '<tr><th><label for="pp-ingest">Διεύθυνση RTMP</label></th><td><input id="pp-ingest" name="ingest_url" class="regular-text code" value="' . esc_attr($s['ingest_url']) . '"></td></tr>';
    echo '<tr><th><label for="pp-watch">Σελίδα παρακολούθησης</label></th><td><input id="pp-watch" name="watch_url" type="url" class="regular-text" value="' . esc_attr($s['watch_url'])
        . '" placeholder="https://www.youtube.com/@…/live"><p class="description">Προαιρετικό. Ο συνεργάτης τη βλέπει για να μοιραστεί τη μετάδοση.</p></td></tr>';
    echo '</table>';
    submit_button();
    echo '</form>';

    echo '<h2>Σύνδεση εφαρμογής</h2><p>Η εφαρμογή χρησιμοποιεί τη διεύθυνση <code>' . esc_html(rest_url('panorama/v1/contributor'))
        . '</code>. Αν την ανοίξετε σε φυλλομετρητή πρέπει να δείτε <code>{"ok":true,"service":"panorama-contributors"}</code>.</p></div>';
}
