package gr.panoramapolihnitou.app.ui.screens.detail

import cafe.adriel.voyager.core.model.ScreenModel
import gr.panoramapolihnitou.app.data.local.BookmarkStore
import gr.panoramapolihnitou.app.data.model.Article
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The caller already has the full [Article] (it came from a list/slider the user
 * tapped), so this screen model just holds it — no need to re-fetch it from the
 * network by id.
 */
class ArticleDetailScreenModel(
    initialArticle: Article,
    val bookmarkStore: BookmarkStore
) : ScreenModel {

    private val _article = MutableStateFlow(initialArticle)
    val article: StateFlow<Article> = _article.asStateFlow()

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}