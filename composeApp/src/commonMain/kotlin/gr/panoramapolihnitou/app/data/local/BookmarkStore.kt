package gr.panoramapolihnitou.app.data.local

import com.russhwolf.settings.Settings
import gr.panoramapolihnitou.app.data.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persists bookmarked articles as a JSON blob in platform key-value storage
 * (SharedPreferences on Android, NSUserDefaults on iOS) via multiplatform-settings.
 * Exposes the current set as a [StateFlow] so screens react to changes instantly.
 */
class BookmarkStore(
    private val settings: Settings = Settings(),
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    private val key = "bookmarked_articles_v1"
    private val serializer = ListSerializer(Article.serializer())

    private val _bookmarks = MutableStateFlow(load())
    val bookmarks: StateFlow<List<Article>> = _bookmarks.asStateFlow()

    private fun load(): List<Article> = runCatching {
        settings.getStringOrNull(key)?.let { json.decodeFromString(serializer, it) }
    }.getOrNull().orEmpty()

    private fun persist(list: List<Article>) {
        settings.putString(key, json.encodeToString(serializer, list))
        _bookmarks.value = list
    }

    fun isBookmarked(id: Long): Boolean = _bookmarks.value.any { it.id == id }

    fun toggle(article: Article) {
        val current = _bookmarks.value
        val updated = if (current.any { it.id == article.id }) {
            current.filterNot { it.id == article.id }
        } else {
            listOf(article) + current
        }
        persist(updated)
    }

    fun remove(id: Long) {
        persist(_bookmarks.value.filterNot { it.id == id })
    }
}
