package gr.panoramapolihnitou.app.ui.screens.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.components.ArticleCard
import gr.panoramapolihnitou.app.ui.components.EmptyView
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.screens.detail.ArticleDetailScreen

class BookmarksScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val store = AppGraph.bookmarkStore
        val bookmarks by store.bookmarks.collectAsState()

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(
                title = "Σελιδοδείκτες",
                showBack = true,
                onBack = { navigator.pop() }
            )
            if (bookmarks.isEmpty()) {
                EmptyView("Δεν έχετε αποθηκεύσει άρθρα.\nΠατήστε το εικονίδιο σελιδοδείκτη σε ένα άρθρο.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(bookmarks, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            isBookmarked = true,
                            onClick = { navigator.push(ArticleDetailScreen(article)) },
                            onToggleBookmark = { store.toggle(article) }
                        )
                    }
                }
            }
        }
    }
}
