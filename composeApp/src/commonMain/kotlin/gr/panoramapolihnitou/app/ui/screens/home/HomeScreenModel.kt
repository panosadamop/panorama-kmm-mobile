package gr.panoramapolihnitou.app.ui.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import gr.panoramapolihnitou.app.data.local.BookmarkStore
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.data.repository.ContentRepository
import gr.panoramapolihnitou.app.ui.UiState
import gr.panoramapolihnitou.app.ui.toUserMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeData(
    val featured: List<Article>,
    val important: List<Article>,
    val latest: List<Article>
)

class HomeScreenModel(
    private val repository: ContentRepository,
    val bookmarkStore: BookmarkStore
) : ScreenModel {

    private val _state = MutableStateFlow<UiState<HomeData>>(UiState.Loading)
    val state: StateFlow<UiState<HomeData>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _endReached = MutableStateFlow(false)
    val endReached: StateFlow<Boolean> = _endReached.asStateFlow()

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    private val pageSize = 15
    private var latestPage = 1

    init {
        load()
    }

    /** Full load — shows the loading spinner (initial load / retry). */
    fun load() {
        _state.value = UiState.Loading
        fetch()
    }

    /** Pull-to-refresh — keeps current content visible, shows the pull indicator. */
    fun refresh() {
        _refreshing.value = true
        fetch()
    }

    private fun fetch() {
        latestPage = 1
        _endReached.value = false
        screenModelScope.launch {
            runCatching {
                val featured = async { repository.getFeatured(limit = 6) }
                val important = async { repository.getImportant(limit = 4) }
                val latest = async { repository.getPosts(page = 1, perPage = pageSize) }
                HomeData(featured.await(), important.await(), latest.await())
            }.onSuccess {
                _endReached.value = it.latest.size < pageSize
                _state.value = UiState.Success(it)
            }.onFailure {
                // Keep showing existing content if a refresh fails.
                if (_state.value !is UiState.Success) _state.value = UiState.Error(it.toUserMessage())
            }
            _refreshing.value = false
        }
    }

    /** Auto-load the next page of "latest" posts when the user nears the bottom. */
    fun loadMore() {
        if (_loadingMore.value || _endReached.value || _refreshing.value) return
        val current = (_state.value as? UiState.Success)?.data ?: return
        _loadingMore.value = true
        latestPage += 1
        screenModelScope.launch {
            runCatching { repository.getPosts(page = latestPage, perPage = pageSize) }
                .onSuccess { more ->
                    _endReached.value = more.size < pageSize
                    // De-dupe in case of overlap between pages.
                    val existingIds = current.latest.mapTo(HashSet()) { it.id }
                    val merged = current.latest + more.filter { it.id !in existingIds }
                    _state.value = UiState.Success(current.copy(latest = merged))
                }
                .onFailure { latestPage -= 1 }
            _loadingMore.value = false
        }
    }

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}
