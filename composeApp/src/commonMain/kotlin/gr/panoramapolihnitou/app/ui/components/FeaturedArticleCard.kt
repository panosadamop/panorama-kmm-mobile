package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.util.formatWpDate
import gr.panoramapolihnitou.app.util.removeGreekAccents

/** Large hero card used to highlight the first article in a category list. */
@Composable
fun FeaturedArticleCard(
    article: Article,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        NetworkImage(
            url = article.imageUrl,
            contentDescription = article.title,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.35f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.85f)
                )
            )
        )
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            article.categoryNames.firstOrNull()?.let {
                Text(
                    text = removeGreekAccents(it).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFFFB3B3)
                )
            }
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = formatWpDate(article.dateIso),
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xCCFFFFFF),
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
