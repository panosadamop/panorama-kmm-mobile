package gr.panoramapolihnitou.app.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.ui.ads.BannerAd
import gr.panoramapolihnitou.app.ui.components.HtmlContent
import gr.panoramapolihnitou.app.ui.components.NetworkImage
import gr.panoramapolihnitou.app.ui.components.PanoramaTopBar
import gr.panoramapolihnitou.app.ui.components.TagPills
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import gr.panoramapolihnitou.app.util.LocalSharer
import gr.panoramapolihnitou.app.util.formatWpDate
import gr.panoramapolihnitou.app.util.removeGreekAccents
import gr.panoramapolihnitou.app.util.stripHtml

data class ArticleDetailScreen(val article: Article) : Screen {

    override val key: String get() = "article-${article.id}"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val sharer = LocalSharer.current
        val model = rememberScreenModel {
            ArticleDetailScreenModel(article, AppGraph.bookmarkStore)
        }
        val current by model.article.collectAsState()
        val bookmarks by model.bookmarks.collectAsState()
        val isBookmarked = bookmarks.any { it.id == current.id }

        Column(Modifier.fillMaxSize()) {
            PanoramaTopBar(
                title = "Άρθρο",
                showBack = true,
                onBack = { navigator.pop() },
                actions = {
                    IconButton(onClick = { model.toggleBookmark(current) }) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Σελιδοδείκτης",
                            tint = PanoramaColors.textWhite
                        )
                    }
                    IconButton(
                        onClick = {
                            sharer.shareText(
                                title = stripHtml(current.title),
                                text = stripHtml(current.title),
                                url = current.link
                            )
                        }
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Κοινοποίηση", tint = PanoramaColors.textWhite)
                    }
                }
            )

            ArticleBody(current)
        }
    }
}

@Composable
private fun ArticleBody(article: Article, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        if (!article.imageUrl.isNullOrBlank()) {
            NetworkImage(
                url = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
            )
        }
        Column(Modifier.padding(16.dp)) {
            article.categoryNames.firstOrNull()?.let {
                Text(
                    text = removeGreekAccents(it).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = PanoramaColors.primary
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            Text(
                text = formatWpDate(article.dateIso),
                style = MaterialTheme.typography.labelMedium,
                color = PanoramaColors.textSecondary,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            HtmlContent(html = article.contentHtml)

            if (article.tagNames.isNotEmpty()) {
                TagSection(article.tagNames)
            }

            BannerAd(Modifier.padding(top = 20.dp))
        }
    }
}

@Composable
private fun TagSection(tags: List<String>) {
    Column(Modifier.padding(top = 20.dp)) {
        Text(
            text = "ΕΤΙΚΕΤΕΣ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = PanoramaColors.textSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        TagPills(tags = tags)
    }
}
