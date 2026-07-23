package gr.panoramapolihnitou.app.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
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
import gr.panoramapolihnitou.app.ui.components.LoadingView
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.screens.detail.ArticleDetailScreen

class SearchScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberScreenModel { SearchScreenModel(AppGraph.repository, AppGraph.bookmarkStore) }
        val state by model.state.collectAsState()
        val bookmarks by model.bookmarks.collectAsState()
        val bookmarkedIds = bookmarks.map { it.id }.toSet()
        var query by remember { mutableStateOf("") }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(
                title = "Αναζήτηση",
                showBack = true,
                onBack = { navigator.pop() }
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { Text("Αναζήτηση άρθρων…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { model.search(query) })
            )

            when (val s = state) {
                null -> EmptyView("Πληκτρολογήστε για αναζήτηση άρθρων.")
                is UiState.Loading -> LoadingView()
                is UiState.Error -> ErrorView(s.message, { model.search(query) })
                is UiState.Success -> if (s.data.isEmpty()) {
                    EmptyView("Δεν βρέθηκαν αποτελέσματα για «$query».")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(s.data, key = { it.id }) { article ->
                            ArticleCard(
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
}
