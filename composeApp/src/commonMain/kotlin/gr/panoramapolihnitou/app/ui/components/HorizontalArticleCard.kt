package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import gr.panoramapolihnitou.app.util.LocalSharer
import gr.panoramapolihnitou.app.util.formatWpDate
import gr.panoramapolihnitou.app.util.removeGreekAccents
import gr.panoramapolihnitou.app.util.stripHtml

/**
 * Compact horizontal list row used for the "ΤΕΛΕΥΤΑΙΑ ΝΕΑ" feed — small image on
 * the left, category + title + date/actions on the right, with a bottom divider
 * (mirrors the reference app's HorizontalCard).
 */
@Composable
fun HorizontalArticleCard(
    article: Article,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onToggleBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharer = LocalSharer.current
    Column(modifier.fillMaxWidth().clickable(onClick = onClick).background(PanoramaColors.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            NetworkImage(
                url = article.imageUrl,
                contentDescription = article.title,
                modifier = Modifier.size(width = 110.dp, height = 80.dp).clip(RoundedCornerShape(6.dp))
            )
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                article.categoryNames.firstOrNull()?.let {
                    Text(
                        text = removeGreekAccents(it).uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = PanoramaColors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = PanoramaColors.text,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (article.tagNames.isNotEmpty()) {
                    TagPills(
                        tags = article.tagNames,
                        max = 3,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatWpDate(article.dateIso),
                        style = MaterialTheme.typography.labelSmall,
                        color = PanoramaColors.textLight
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                sharer.shareText(stripHtml(article.title), stripHtml(article.title), article.link)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "Κοινοποίηση",
                                tint = PanoramaColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(onClick = onToggleBookmark, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Σελιδοδείκτης",
                                tint = if (isBookmarked) PanoramaColors.primary else PanoramaColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = PanoramaColors.border)
    }
}
