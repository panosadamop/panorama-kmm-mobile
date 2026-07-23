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

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    init {
        load()
    }

    fun load() {
        _state.value = UiState.Loading
        screenModelScope.launch {
            runCatching {
                val featured = async { repository.getFeatured(limit = 6) }
                val important = async { repository.getImportant(limit = 4) }
                val latest = async { repository.getPosts(page = 1, perPage = 15) }
                HomeData(featured.await(), important.await(), latest.await())
            }.onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}
