<?php
/**
 * Πανόραμα Πολιχνίτου — push notification on publish (Firebase Cloud Messaging).
 *
 * Sends one FCM message to the `new_articles` topic the first time a post is
 * published (immediately or when a scheduled post goes live). Every install of
 * the app subscribes to that topic, so there is no device-token database.
 * Tapping the notification opens the article (the app reads `post_id`).
 *
 * FCM is free with no message limits. Dependency-free: signs the OAuth JWT with
 * PHP's OpenSSL, so it works on shared hosting without Composer.
 *
 * SETUP
 *  1. Firebase console → project "panorama-polihnitou-notificy" → ⚙ Project settings
 *     → Service accounts → "Generate new private key". Downloads a JSON file.
 *  2. Upload that JSON to the server OUTSIDE the public web root, e.g.
 *     /home/<account>/private/firebase-service-account.json, and set the path below.
 *     Never commit it or put it under public_html: it can send pushes to every user.
 *  3. Install this snippet with the "Code Snippets" plugin (Run everywhere), or
 *     paste it into the child theme's functions.php.
 *  4. Publish a test post. Check wp-content/debug.log (with WP_DEBUG_LOG on) if
 *     nothing arrives.
 *
 * To skip the push for a particular post, add the custom field
 * `_panorama_no_push` = 1 before publishing.
 */

if (!defined('ABSPATH')) {
    exit;
}

define('PANORAMA_FCM_SERVICE_ACCOUNT', '/home/CHANGE-ME/private/firebase-service-account.json');
define('PANORAMA_FCM_TOPIC', 'new_articles');        // must match the apps
define('PANORAMA_FCM_ANDROID_CHANNEL', 'new_articles');

add_action('transition_post_status', 'panorama_fcm_on_publish', 10, 3);

function panorama_fcm_on_publish($new_status, $old_status, $post)
{
    if ($new_status !== 'publish' || $old_status === 'publish') return;
    if ($post->post_type !== 'post') return;
    if (wp_is_post_revision($post) || wp_is_post_autosave($post)) return;
    if (get_post_meta($post->ID, '_panorama_no_push', true)) return;
    // Guards against a second push if a post is unpublished and published again.
    if (get_post_meta($post->ID, '_panorama_push_sent', true)) return;
    update_post_meta($post->ID, '_panorama_push_sent', time());

    $title = html_entity_decode(wp_strip_all_tags(get_the_title($post)), ENT_QUOTES, 'UTF-8');
    panorama_fcm_send_to_topic('Πανόραμα Πολιχνίτου', $title, array('post_id' => (string) $post->ID));
}

function panorama_fcm_send_to_topic($title, $body, array $data)
{
    $account = panorama_fcm_service_account();
    if (!$account) return false;
    $token = panorama_fcm_access_token($account);
    if (!$token) return false;

    $message = array(
        'message' => array(
            'topic' => PANORAMA_FCM_TOPIC,
            'notification' => array('title' => $title, 'body' => $body),
            'data' => $data, // values must be strings
            'android' => array(
                'priority' => 'high',
                'notification' => array('channel_id' => PANORAMA_FCM_ANDROID_CHANNEL),
            ),
            'apns' => array(
                'payload' => array('aps' => array('sound' => 'default')),
            ),
        ),
    );

    $response = wp_remote_post(
        'https://fcm.googleapis.com/v1/projects/' . rawurlencode($account['project_id']) . '/messages:send',
        array(
            'timeout' => 10,
            'headers' => array(
                'Authorization' => 'Bearer ' . $token,
                'Content-Type' => 'application/json; charset=UTF-8',
            ),
            'body' => wp_json_encode($message),
        )
    );
    if (is_wp_error($response) || wp_remote_retrieve_response_code($response) !== 200) {
        error_log('[panorama-fcm] send failed: ' . (is_wp_error($response)
            ? $response->get_error_message()
            : wp_remote_retrieve_body($response)));
        return false;
    }
    return true;
}

function panorama_fcm_service_account()
{
    $path = PANORAMA_FCM_SERVICE_ACCOUNT;
    if (!is_readable($path)) {
        error_log('[panorama-fcm] service account file not readable: ' . $path);
        return null;
    }
    $account = json_decode(file_get_contents($path), true);
    if (empty($account['client_email']) || empty($account['private_key']) || empty($account['project_id'])) {
        error_log('[panorama-fcm] service account file is missing fields');
        return null;
    }
    return $account;
}

/** OAuth2 access token for FCM, cached for 50 of its 60 minutes. */
function panorama_fcm_access_token(array $account)
{
    $cached = get_transient('panorama_fcm_token');
    if ($cached) return $cached;

    $now = time();
    $header = panorama_fcm_b64url(wp_json_encode(array('alg' => 'RS256', 'typ' => 'JWT')));
    $claims = panorama_fcm_b64url(wp_json_encode(array(
        'iss' => $account['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'iat' => $now,
        'exp' => $now + 3600,
    )));
    $signature = '';
    if (!openssl_sign($header . '.' . $claims, $signature, $account['private_key'], OPENSSL_ALGO_SHA256)) {
        error_log('[panorama-fcm] could not sign JWT (is the OpenSSL extension enabled?)');
        return null;
    }
    $jwt = $header . '.' . $claims . '.' . panorama_fcm_b64url($signature);

    $response = wp_remote_post('https://oauth2.googleapis.com/token', array(
        'timeout' => 10,
        'body' => array(
            'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
            'assertion' => $jwt,
        ),
    ));
    if (is_wp_error($response)) {
        error_log('[panorama-fcm] token request failed: ' . $response->get_error_message());
        return null;
    }
    $body = json_decode(wp_remote_retrieve_body($response), true);
    if (empty($body['access_token'])) {
        error_log('[panorama-fcm] token response: ' . wp_remote_retrieve_body($response));
        return null;
    }
    set_transient('panorama_fcm_token', $body['access_token'], 50 * MINUTE_IN_SECONDS);
    return $body['access_token'];
}

function panorama_fcm_b64url($data)
{
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}
