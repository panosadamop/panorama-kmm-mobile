package gr.panoramapolihnitou.app.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tapping a "new article" push opens that article. The platforms only know the
 * WordPress post id (the `post_id` data field sent by
 * `wordpress-integration/push-on-publish.php`); [App] fetches and navigates once
 * the splash is gone. Called from MainActivity (Android) and AppDelegate (iOS,
 * as `DeepLinks.shared.openArticle(postId:)`).
 */
object DeepLinks {
    private val _pendingArticleId = MutableStateFlow<Long?>(null)
    val pendingArticleId: StateFlow<Long?> = _pendingArticleId.asStateFlow()

    fun openArticle(postId: String?) {
        postId?.trim()?.toLongOrNull()?.let { _pendingArticleId.value = it }
    }

    fun consume() {
        _pendingArticleId.value = null
    }
}
