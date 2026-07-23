package gr.panoramapolihnitou.app.ui.screens.articles

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

class ArticleListScreenModel(
    private val categoryId: Long?,
    private val repository: ContentRepository,
    val bookmarkStore: BookmarkStore
) : ScreenModel {

    private val pageSize = 10

    private val _state = MutableStateFlow<UiState<List<Article>>>(UiState.Loading)
    val state: StateFlow<UiState<List<Article>>> = _state.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    private var page = 1
    private var query: String? = null
    private val accumulated = mutableListOf<Article>()

    init {
        refresh()
    }

    fun setSearch(text: String) {
        query = text.trim().ifBlank { null }
        refresh()
    }

    fun refresh() {
        page = 1
        _endReached.value = false
        accumulated.clear()
        _state.value = UiState.Loading
        screenModelScope.launch {
            runCatching { repository.getPosts(page, pageSize, categoryId, query) }
                .onSuccess {
                    accumulated += it
                    _endReached.value = it.size < pageSize
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }

    /** Pull-to-refresh: reload page 1 without blanking the current list. */
    fun pullRefresh() {
        _refreshing.value = true
        page = 1
        _endReached.value = false
        screenModelScope.launch {
            runCatching { repository.getPosts(page, pageSize, categoryId, query) }
                .onSuccess {
                    accumulated.clear()
                    accumulated += it
                    _endReached.value = it.size < pageSize
                    _state.value = UiState.Success(accumulated.toList())
                }
            _refreshing.value = false
        }
    }

    fun loadMore() {
        if (_loadingMore.value || _endReached.value) return
        if (_state.value !is UiState.Success) return
        _loadingMore.value = true
        page += 1
        screenModelScope.launch {
            runCatching { repository.getPosts(page, pageSize, categoryId, query) }
                .onSuccess {
                    accumulated += it
                    _endReached.value = it.size < pageSize
                    _state.value = UiState.Success(accumulated.toList())
                }
                .onFailure { page -= 1 }
            _loadingMore.value = false
        }
    }

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}
