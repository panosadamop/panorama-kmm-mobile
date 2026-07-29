package gr.panoramapolihnitou.app.ui.screens.articles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.UiState
import gr.panoramapolihnitou.app.ui.components.ArticleCard
import gr.panoramapolihnitou.app.ui.components.EmptyView
import gr.panoramapolihnitou.app.ui.components.ErrorView
import gr.panoramapolihnitou.app.ui.components.FeaturedSlider
import gr.panoramapolihnitou.app.ui.components.LoadingView
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.screens.detail.ArticleDetailScreen

data class ArticleListScreen(
    val categoryId: Long?,
    val title: String
) : Screen {

    override val key: String get() = "articles-${categoryId ?: "all"}"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel {
            ArticleListScreenModel(categoryId, AppGraph.repository, AppGraph.bookmarkStore)
        }
        val state by model.state.collectAsState()
        val loadingMore by model.loadingMore.collectAsState()
        val refreshing by model.refreshing.collectAsState()
        val bookmarks by model.bookmarks.collectAsState()
        val bookmarkedIds = bookmarks.map { it.id }.toSet()
        val listState = rememberLazyListState()

        // Infinite scroll: load more when nearing the end.
        val shouldLoadMore by remember {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val total = listState.layoutInfo.totalItemsCount
                total > 0 && last >= total - 3
            }
        }
        LaunchedEffect(shouldLoadMore) {
            if (shouldLoadMore) model.loadMore()
        }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(title = title, showBack = true, onBack = { navigator.pop() })
            when (val s = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, model::refresh)
                is UiState.Success -> if (s.data.isEmpty()) {
                    EmptyView("Δεν βρέθηκαν άρθρα.")
                } else PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = model::pullRefresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Top 3 latest articles of the category as a slider.
                        val top = s.data.take(3)
                        if (top.isNotEmpty()) {
                            item(key = "cat-slider") {
                                FeaturedSlider(
                                    articles = top,
                                    onArticleClick = { navigator.push(ArticleDetailScreen(it)) }
                                )
                            }
                        }
                        // The rest as standard cards.
                        items(s.data.drop(3), key = { it.id }) { article ->
                            ArticleCard(
                                article = article,
                                isBookmarked = article.id in bookmarkedIds,
                                onClick = { navigator.push(ArticleDetailScreen(article)) },
                                onToggleBookmark = { model.toggleBookmark(article) },
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        if (loadingMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }
}
