package gr.panoramapolihnitou.app.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.UiState
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
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel {
            HomeScreenModel(AppGraph.repository, AppGraph.bookmarkStore)
        }
        val state by model.state.collectAsState()
        val bookmarks by model.bookmarks.collectAsState()
        val bookmarkedIds = bookmarks.map { it.id }.toSet()

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(
                title = "Πανόραμα Πολιχνίτου",
                showSearch = true,
                onSearch = { navigator.push(SearchScreen()) }
            )
            when (val s = state) {
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, model::load)
                is UiState.Success -> LazyColumn(
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
                }
            }
        }
    }
}
