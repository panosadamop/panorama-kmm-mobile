package gr.panoramapolihnitou.app.ui.screens.bookmarks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.components.ArticleCard
import gr.panoramapolihnitou.app.ui.components.EmptyView
import gr.panoramapolihnitou.app.ui.screens.detail.ArticleDetailScreen

class BookmarksScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val store = AppGraph.bookmarkStore
        val bookmarks by store.bookmarks.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Σελιδοδείκτες", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            if (bookmarks.isEmpty()) {
                EmptyView(
                    "Δεν έχετε αποθηκεύσει άρθρα.\nΠατήστε το εικονίδιο σελιδοδείκτη σε ένα άρθρο.",
                    Modifier.padding(padding)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        top = padding.calculateTopPadding() + 12.dp,
                        bottom = padding.calculateBottomPadding() + 16.dp,
                        start = 16.dp,
                        end = 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(bookmarks, key = { it.id }) { article ->
                        ArticleCard(
                            article = article,
                            isBookmarked = true,
                            onClick = { navigator.push(ArticleDetailScreen(article.id)) },
                            onToggleBookmark = { store.toggle(article) }
                        )
                    }
                }
            }
        }
    }
}
