package gr.panoramapolihnitou.app.util

/**
 * Drops Greek tonal accents so text can be shown in ALL CAPS the conventional
 * way (e.g. "Άρθρο" → "ΑΡΘΡΟ", not "ΆΡΘΡΟ"). Common code has no Unicode NFD
 * normalisation, so we map the accented Greek letters explicitly.
 */
private val greekAccentMap = mapOf(
    'ά' to 'α', 'έ' to 'ε', 'ή' to 'η', 'ί' to 'ι', 'ό' to 'ο', 'ύ' to 'υ', 'ώ' to 'ω',
    'ϊ' to 'ι', 'ϋ' to 'υ', 'ΐ' to 'ι', 'ΰ' to 'υ',
    'Ά' to 'Α', 'Έ' to 'Ε', 'Ή' to 'Η', 'Ί' to 'Ι', 'Ό' to 'Ο', 'Ύ' to 'Υ', 'Ώ' to 'Ω',
    'Ϊ' to 'Ι', 'Ϋ' to 'Υ'
)

fun removeGreekAccents(text: String): String =
    buildString(text.length) {
        for (ch in text) append(greekAccentMap[ch] ?: ch)
    }
