package gr.panoramapolihnitou.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.graphics.Color

/**
 * A very small, dependency-free HTML handler tailored to WordPress post content.
 * It is intentionally forgiving: anything it doesn't understand degrades to plain
 * text rather than throwing. Two entry points:
 *  - [stripHtml] for excerpts / previews (plain text)
 *  - [parseHtmlToBlocks] for the article detail screen (styled blocks + images)
 */

sealed interface ContentBlock {
    data class Paragraph(val text: AnnotatedString) : ContentBlock
    data class Heading(val text: AnnotatedString, val level: Int) : ContentBlock
    data class Bullet(val text: AnnotatedString) : ContentBlock
    data class Quote(val text: AnnotatedString) : ContentBlock
    data class Image(val url: String, val alt: String?) : ContentBlock
}

private val entityMap = mapOf(
    "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"",
    "&#039;" to "'", "&#39;" to "'", "&apos;" to "'", "&nbsp;" to " ",
    "&hellip;" to "…", "&ndash;" to "–", "&mdash;" to "—",
    "&laquo;" to "«", "&raquo;" to "»", "&rsquo;" to "’", "&lsquo;" to "‘",
    "&ldquo;" to "“", "&rdquo;" to "”", "&euro;" to "€", "&copy;" to "©",
    "&middot;" to "·", "&bull;" to "•"
)

private val numericEntity = Regex("&#(\\d+);")
private val hexEntity = Regex("&#x([0-9a-fA-F]+);")

fun decodeEntities(input: String): String {
    var s = input
    entityMap.forEach { (k, v) -> s = s.replace(k, v) }
    s = numericEntity.replace(s) { m ->
        m.groupValues[1].toIntOrNull()?.let { StringBuilder().appendCodePointCompat(it).toString() } ?: m.value
    }
    s = hexEntity.replace(s) { m ->
        m.groupValues[1].toIntOrNull(16)?.let { StringBuilder().appendCodePointCompat(it).toString() } ?: m.value
    }
    return s
}

private fun StringBuilder.appendCodePointCompat(cp: Int): StringBuilder {
    if (cp <= 0xFFFF) append(cp.toChar()) else {
        val v = cp - 0x10000
        append((0xD800 + (v shr 10)).toChar())
        append((0xDC00 + (v and 0x3FF)).toChar())
    }
    return this
}

private val tagRegex = Regex("<[^>]+>")

/** Removes all tags, decodes entities, and collapses whitespace. */
fun stripHtml(html: String): String =
    decodeEntities(tagRegex.replace(html, " "))
        .replace(Regex("\\s+"), " ")
        .trim()

private val LinkColor = Color(0xFF1565C0)

/**
 * Converts an inline HTML fragment (no block tags) into a styled AnnotatedString.
 * Handles <strong>/<b>, <em>/<i>, and <a href>. Tappable links open via
 * LocalUriHandler automatically thanks to [LinkAnnotation.Url].
 */
fun inlineHtmlToAnnotated(fragment: String): AnnotatedString = buildAnnotatedString {
    val token = Regex("<(/?)(strong|b|em|i|a)([^>]*)>", RegexOption.IGNORE_CASE)
    var index = 0
    var bold = 0
    var italic = 0
    val hrefStack = ArrayDeque<String>()

    fun appendText(raw: String) {
        if (raw.isEmpty()) return
        val text = decodeEntities(raw)
        val style = SpanStyle(
            fontWeight = if (bold > 0) FontWeight.Bold else null,
            fontStyle = if (italic > 0) FontStyle.Italic else null
        )
        val href = hrefStack.lastOrNull()
        if (href != null) {
            withLink(
                LinkAnnotation.Url(
                    href,
                    styles = TextLinkStyles(
                        style = SpanStyle(color = LinkColor, textDecoration = TextDecoration.Underline)
                    )
                )
            ) { pushStyle(style); append(text); pop() }
        } else {
            pushStyle(style); append(text); pop()
        }
    }

    for (m in token.findAll(fragment)) {
        appendText(fragment.substring(index, m.range.first))
        index = m.range.last + 1
        val closing = m.groupValues[1] == "/"
        val tag = m.groupValues[2].lowercase()
        val attrs = m.groupValues[3]
        when (tag) {
            "strong", "b" -> if (closing) bold = (bold - 1).coerceAtLeast(0) else bold++
            "em", "i" -> if (closing) italic = (italic - 1).coerceAtLeast(0) else italic++
            "a" -> if (closing) {
                if (hrefStack.isNotEmpty()) hrefStack.removeLast()
            } else {
                val href = Regex("href\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
                    .find(attrs)?.groupValues?.get(1)
                hrefStack.addLast(href.orEmpty().ifBlank { "" })
            }
        }
    }
    appendText(fragment.substring(index))
}

private val imgSrc = Regex("<img[^>]*?src\\s*=\\s*\"([^\"]+)\"[^>]*>", RegexOption.IGNORE_CASE)
private val imgAlt = Regex("alt\\s*=\\s*\"([^\"]*)\"", RegexOption.IGNORE_CASE)
private val blockSplit = Regex("</?(p|div|h[1-6]|li|blockquote|ul|ol|figure|br\\s*/?)[^>]*>", RegexOption.IGNORE_CASE)

/**
 * Splits post HTML into renderable blocks. Images are lifted out as standalone
 * [ContentBlock.Image] blocks; remaining text is grouped by block-level tags.
 */
fun parseHtmlToBlocks(html: String): List<ContentBlock> {
    val blocks = mutableListOf<ContentBlock>()
    var cursor = 0
    // Walk images in order, emitting text-between as paragraphs.
    for (m in imgSrc.findAll(html)) {
        val before = html.substring(cursor, m.range.first)
        blocks += textToBlocks(before)
        val url = m.groupValues[1]
        val alt = imgAlt.find(m.value)?.groupValues?.get(1)
        if (url.isNotBlank()) blocks += ContentBlock.Image(url, alt)
        cursor = m.range.last + 1
    }
    blocks += textToBlocks(html.substring(cursor))
    return blocks.ifEmpty {
        listOf(ContentBlock.Paragraph(inlineHtmlToAnnotated(html)))
    }
}

private fun textToBlocks(chunk: String): List<ContentBlock> {
    if (chunk.isBlank()) return emptyList()
    val result = mutableListOf<ContentBlock>()
    // Detect headings / list items / quotes by their opening tag, else paragraph.
    val segments = chunk.split(blockSplit)
    // Track the most recent opening tag to classify the following segment.
    val openings = blockSplit.findAll(chunk).map { it.value.lowercase() }.toList()
    var tagIndex = -1
    for (segment in segments) {
        val raw = segment.trim()
        val opener = openings.getOrNull(tagIndex)
        tagIndex++
        if (raw.isEmpty()) continue
        val annotated = inlineHtmlToAnnotated(raw)
        if (annotated.text.isBlank()) continue
        when {
            opener?.contains(Regex("h[1-6]")) == true -> {
                val level = Regex("h([1-6])").find(opener)?.groupValues?.get(1)?.toIntOrNull() ?: 2
                result += ContentBlock.Heading(annotated, level)
            }
            opener?.contains("li") == true -> result += ContentBlock.Bullet(annotated)
            opener?.contains("blockquote") == true -> result += ContentBlock.Quote(annotated)
            else -> result += ContentBlock.Paragraph(annotated)
        }
    }
    return result
}
