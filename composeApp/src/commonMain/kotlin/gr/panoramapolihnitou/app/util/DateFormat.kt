package gr.panoramapolihnitou.app.util

private val greekMonths = arrayOf(
    "Ιανουαρίου", "Φεβρουαρίου", "Μαρτίου", "Απριλίου", "Μαΐου", "Ιουνίου",
    "Ιουλίου", "Αυγούστου", "Σεπτεμβρίου", "Οκτωβρίου", "Νοεμβρίου", "Δεκεμβρίου"
)

/**
 * Formats a WordPress ISO-8601 date (e.g. "2024-05-01T10:30:00") as
 * "1 Μαΐου 2024". No external date library needed — the format is fixed.
 */
fun formatWpDate(iso: String): String {
    val datePart = iso.substringBefore('T')
    val pieces = datePart.split('-')
    if (pieces.size != 3) return datePart
    val year = pieces[0]
    val month = pieces[1].toIntOrNull() ?: return datePart
    val day = pieces[2].toIntOrNull() ?: return datePart
    val monthName = greekMonths.getOrNull(month - 1) ?: pieces[1]
    return "$day $monthName $year"
}
