package gr.panoramapolihnitou.app.ui

/** Simple three-state wrapper used by every screen model. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}

/** Human-friendly message for network/parse failures. */
fun Throwable.toUserMessage(): String = when (this) {
    else -> "Δεν ήταν δυνατή η φόρτωση του περιεχομένου. Ελέγξτε τη σύνδεσή σας και δοκιμάστε ξανά."
}
