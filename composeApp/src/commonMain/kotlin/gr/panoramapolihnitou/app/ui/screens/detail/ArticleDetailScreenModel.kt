package gr.panoramapolihnitou.app.ui.screens.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import gr.panoramapolihnitou.app.data.local.BookmarkStore
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.data.repository.ContentRepository
import gr.panoramapolihnitou.app.ui.UiState
import gr.panoramapolihnitou.app.ui.toUserMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ArticleDetailScreenModel(
    private val articleId: Long,
    private val repository: ContentRepository,
    val bookmarkStore: BookmarkStore
) : ScreenModel {

    private val _state = MutableStateFlow<UiState<Article>>(UiState.Loading)
    val state: StateFlow<UiState<Article>> = _state.asStateFlow()

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        screenModelScope.launch {
            runCatching { repository.getArticle(articleId) }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}
