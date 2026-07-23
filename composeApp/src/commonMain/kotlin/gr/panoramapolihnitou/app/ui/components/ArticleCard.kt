package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.util.formatWpDate
import gr.panoramapolihnitou.app.util.removeGreekAccents
import gr.panoramapolihnitou.app.util.stripHtml

/** Card used across the home feed, article lists, bookmarks and search results. */
@Composable
fun ArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        NetworkImage(
            url = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        Column(Modifier.padding(14.dp)) {
            val category = article.categoryNames.firstOrNull()
            if (category != null) {
                Text(
                    text = removeGreekAccents(category).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            val excerpt = stripHtml(article.excerptHtml)
            if (excerpt.isNotBlank()) {
                Text(
                    text = excerpt,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            if (article.tagNames.isNotEmpty()) {
                TagPills(
                    tags = article.tagNames,
                    max = 3,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatWpDate(article.dateIso),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onToggleBookmark, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (isBookmarked) "Αφαίρεση σελιδοδείκτη" else "Προσθήκη σελιδοδείκτη",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Suppress("unused")
private val cardShape = RoundedCornerShape(12.dp)

/** Compact one-line list row (used in dense lists if needed). */
@Composable
fun ArticleRow(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        NetworkImage(
            url = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier.size(72.dp).clip(RoundedCornerShape(8.dp))
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatWpDate(article.dateIso),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
