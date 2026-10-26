<?php
/**
 * Removes the partner list, its secret and the settings (incl. the stream key).
 * Submissions already received stay: they are ordinary posts + Media Library files.
 */
if (!defined('WP_UNINSTALL_PLUGIN')) {
    exit;
}
global $wpdb;
$wpdb->query('DROP TABLE IF EXISTS ' . $wpdb->prefix . 'panorama_partners');
delete_option('panorama_partners_settings');
delete_option('panorama_partners_secret');
delete_option('panorama_partners_db_version');
