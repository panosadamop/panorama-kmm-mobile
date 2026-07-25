package gr.panoramapolihnitou.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.ui.theme.PanoramaColors
import gr.panoramapolihnitou.app.util.formatWpDate
import gr.panoramapolihnitou.app.util.removeGreekAccents

private val SlideHeight = 280.dp

/**
 * Full-bleed featured slider — edge-to-edge, no rounded corners — mirroring the
 * homepage slider on panoramapolihnitou.gr: cover image, bottom gradient, red
 * category badge, white title, pill dot indicators.
 */
@Composable
fun FeaturedSlider(
    articles: List<Article>,
    onArticleClick: (Article) -> Unit,
    modifier: Modifier = Modifier
) {
    if (articles.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { articles.size })

    Box(modifier.fillMaxWidth().height(SlideHeight)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val article = articles[page]
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable { onArticleClick(article) }
            ) {
                NetworkImage(
                    url = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier.fillMaxSize()
                )
                // Bottom gradient scrim.
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.35f to Color.Transparent,
                            0.7f to Color.Black.copy(alpha = 0.40f),
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
                ) {
                    val category = article.categoryNames.firstOrNull()
                    if (category != null) {
                        Text(
                            text = removeGreekAccents(category).uppercase(),
                            style = TextStyle(
                                color = PanoramaColors.textWhite,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                lineHeight = 10.sp,
                                lineHeightStyle = LineHeightStyle(
                                    alignment = LineHeightStyle.Alignment.Center,
                                    trim = LineHeightStyle.Trim.Both
                                )
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PanoramaColors.primary)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = article.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = formatWpDate(article.dateIso),
                        color = Color(0xBFFFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        }

        // Pill dot indicators, overlaid at the bottom-center.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(articles.size) { i ->
                val selected = i == pagerState.currentPage
                val width by animateDpAsState(if (selected) 20.dp else 6.dp)
                Box(
                    Modifier
                        .padding(horizontal = 3.dp)
                        .size(width = width, height = 6.dp)
                        .clip(if (selected) RoundedCornerShape(3.dp) else CircleShape)
                        .background(if (selected) Color.White else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}
