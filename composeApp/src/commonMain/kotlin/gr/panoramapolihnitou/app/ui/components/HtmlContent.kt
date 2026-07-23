package gr.panoramapolihnitou.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import gr.panoramapolihnitou.app.util.ContentBlock
import gr.panoramapolihnitou.app.util.parseHtmlToBlocks

/**
 * Renders WordPress post HTML as native Compose: styled paragraphs/headings,
 * bullet lists, block quotes and inline images. Links inside text are tappable
 * (opened by LocalUriHandler through the parser's LinkAnnotation).
 */
@Composable
fun HtmlContent(
    html: String,
    modifier: Modifier = Modifier
) {
    val blocks = remember(html) { parseHtmlToBlocks(html) }
    Column(modifier.fillMaxWidth()) {
        blocks.forEach { block ->
            when (block) {
                is ContentBlock.Paragraph -> Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                )

                is ContentBlock.Heading -> Text(
                    text = block.text,
                    style = when (block.level) {
                        1 -> MaterialTheme.typography.headlineMedium
                        2 -> MaterialTheme.typography.headlineSmall
                        else -> MaterialTheme.typography.titleLarge
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp)
                )

                is ContentBlock.Bullet -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp, horizontal = 4.dp)
                ) {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(block.text, style = MaterialTheme.typography.bodyLarge)
                }

                is ContentBlock.Quote -> Box(
                    Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    Text(
                        text = block.text,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                    )
                }

                is ContentBlock.Image -> NetworkImage(
                    url = block.url,
                    contentDescription = block.alt,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            }
        }
    }
}
