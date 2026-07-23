package gr.panoramapolihnitou.app.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.UiState
import gr.panoramapolihnitou.app.ui.ads.BannerAd
import gr.panoramapolihnitou.app.ui.components.ArticleCard
import gr.panoramapolihnitou.app.ui.components.ErrorView
import gr.panoramapolihnitou.app.ui.components.FeaturedSlider
import gr.panoramapolihnitou.app.ui.components.HorizontalArticleCard
import gr.panoramapolihnitou.app.ui.components.LoadingView
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.components.SectionHeader
import gr.panoramapolihnitou.app.ui.screens.detail.ArticleDetailScreen
import gr.panoramapolihnitou.app.ui.screens.search.SearchScreen

class HomeScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel {
            HomeScreenModel(AppGraph.repository, AppGraph.bookmarkStore)
        }
        val state by model.state.collectAsState()
        val refreshing by model.refreshing.collectAsState()
        val loadingMore by model.loadingMore.collectAsState()
        val bookmarks by model.bookmarks.collectAsState()
        val bookmarkedIds = bookmarks.map { it.id }.toSet()
        val listState = rememberLazyListState()

        // Auto-load more when the last few items become visible.
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
            PanoramaTopBar(
                title = "Πανόραμα Πολιχνίτου",
                showSearch = true,
                onSearch = { navigator.push(SearchScreen()) }
            )
            when (val s = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, model::load)
                is UiState.Success -> PullToRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = model::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                    if (s.data.featured.isNotEmpty()) {
                        item {
                            FeaturedSlider(
                                articles = s.data.featured,
                                onArticleClick = { navigator.push(ArticleDetailScreen(it.id)) }
                            )
                        }
                    }
                    // ΣΗΜΑΝΤΙΚΑ — large image-on-top cards.
                    if (s.data.important.isNotEmpty()) {
                        item { SectionHeader("Σημαντικά") }
                        items(s.data.important, key = { "imp-${it.id}" }) { article ->
                            ArticleCard(
                                article = article,
                                isBookmarked = article.id in bookmarkedIds,
                                onClick = { navigator.push(ArticleDetailScreen(article.id)) },
                                onToggleBookmark = { model.toggleBookmark(article) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                    // Banner ad between sections.
                    item { BannerAd(Modifier.padding(vertical = 8.dp)) }

                    // ΤΕΛΕΥΤΑΙΑ ΝΕΑ — compact horizontal rows with dividers.
                    item { SectionHeader("Τελευταία Νέα") }
                    items(s.data.latest, key = { it.id }) { article ->
                        HorizontalArticleCard(
                            article = article,
                            isBookmarked = article.id in bookmarkedIds,
                            onClick = { navigator.push(ArticleDetailScreen(article.id)) },
                            onToggleBookmark = { model.toggleBookmark(article) }
                        )
                    }

                        // Loading indicator while auto-loading more.
                        if (loadingMore) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) { CircularProgressIndicator() }
                            }
                        }

                        // Banner ad at the end of the feed.
                        item { BannerAd(Modifier.padding(vertical = 12.dp)) }
                    }
                }
            }
        }
    }
}
