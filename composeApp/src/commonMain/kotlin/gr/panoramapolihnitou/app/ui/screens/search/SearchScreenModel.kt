package gr.panoramapolihnitou.app.ui.screens.search

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

class SearchScreenModel(
    private val repository: ContentRepository,
    val bookmarkStore: BookmarkStore
) : ScreenModel {

    // Null state = "no search yet" (initial empty prompt).
    private val _state = MutableStateFlow<UiState<List<Article>>?>(null)
    val state: StateFlow<UiState<List<Article>>?> = _state.asStateFlow()

    val bookmarks: StateFlow<List<Article>> = bookmarkStore.bookmarks

    fun search(query: String) {
        val q = query.trim()
        if (q.isBlank()) {
            _state.value = null
            return
        }
        _state.value = UiState.Loading
        screenModelScope.launch {
            runCatching { repository.getPosts(page = 1, perPage = 20, search = q) }
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { _state.value = UiState.Error(it.toUserMessage()) }
        }
    }

    fun toggleBookmark(article: Article) = bookmarkStore.toggle(article)
}
