package gr.panoramapolihnitou.app.data.remote

import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.data.model.Category
import gr.panoramapolihnitou.app.data.model.StaticPage
import gr.panoramapolihnitou.app.util.stripHtml

/** Picks the best available featured-image URL from the embedded media. */
private fun PostDto.resolveImageUrl(): String? {
    val media = embedded?.featuredMedia?.firstOrNull() ?: return null
    // Prefer a medium-large size for cards; fall back to the full source.
    val sizes = media.mediaDetails?.sizes.orEmpty()
    return sizes["medium_large"]?.sourceUrl
        ?: sizes["large"]?.sourceUrl
        ?: sizes["medium"]?.sourceUrl
        ?: media.sourceUrl
}

/** Category names attached to the post via the embedded `wp:term` taxonomy. */
private fun PostDto.resolveCategoryNames(): List<String> =
    embedded?.terms.orEmpty()
        .flatten()
        .filter { it.taxonomy == "category" }
        .map { stripHtml(it.name) }
        .filter { it.isNotBlank() }
        .distinct()

/** Tag names (taxonomy `post_tag`) attached via the embedded `wp:term`. */
private fun PostDto.resolveTagNames(): List<String> =
    embedded?.terms.orEmpty()
        .flatten()
        .filter { it.taxonomy == "post_tag" }
        .map { stripHtml(it.name) }
        .filter { it.isNotBlank() }
        .distinct()

fun PostDto.toArticle(): Article = Article(
    id = id,
    title = stripHtml(title.rendered),
    excerptHtml = excerpt.rendered,
    excerptText = stripHtml(excerpt.rendered),
    contentHtml = content.rendered,
    dateIso = date,
    link = link,
    imageUrl = resolveImageUrl(),
    categoryIds = categories,
    categoryNames = resolveCategoryNames(),
    tagNames = resolveTagNames()
)

fun CategoryDto.toCategory(): Category = Category(
    id = id,
    name = stripHtml(name),
    count = count,
    parent = parent
)

fun PageDto.toStaticPage(): StaticPage = StaticPage(
    id = id,
    title = stripHtml(title.rendered),
    contentHtml = content.rendered,
    link = link
)
