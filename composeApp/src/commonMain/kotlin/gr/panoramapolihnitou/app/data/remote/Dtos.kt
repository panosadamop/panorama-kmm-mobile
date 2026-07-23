package gr.panoramapolihnitou.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * WordPress REST API v2 data-transfer objects.
 * Only the fields the app consumes are declared; unknown keys are ignored
 * (see [ApiJson]). Posts/pages are requested with `?_embed` so featured media
 * and taxonomy terms arrive inline under `_embedded`.
 */

@Serializable
data class RenderedField(
    @SerialName("rendered") val rendered: String = ""
)

@Serializable
data class PostDto(
    val id: Long = 0,
    val date: String = "",
    val link: String = "",
    val title: RenderedField = RenderedField(),
    val excerpt: RenderedField = RenderedField(),
    val content: RenderedField = RenderedField(),
    @SerialName("featured_media") val featuredMedia: Long = 0,
    val categories: List<Long> = emptyList(),
    @SerialName("_embedded") val embedded: EmbeddedDto? = null
)

@Serializable
data class EmbeddedDto(
    @SerialName("wp:featuredmedia") val featuredMedia: List<FeaturedMediaDto> = emptyList(),
    @SerialName("wp:term") val terms: List<List<TermDto>> = emptyList()
)

@Serializable
data class FeaturedMediaDto(
    @SerialName("source_url") val sourceUrl: String? = null,
    @SerialName("media_details") val mediaDetails: MediaDetailsDto? = null
)

@Serializable
data class MediaDetailsDto(
    val sizes: Map<String, MediaSizeDto> = emptyMap()
)

@Serializable
data class MediaSizeDto(
    @SerialName("source_url") val sourceUrl: String? = null
)

@Serializable
data class TermDto(
    val id: Long = 0,
    val name: String = "",
    val taxonomy: String = ""
)

@Serializable
data class CategoryDto(
    val id: Long = 0,
    val name: String = "",
    val count: Int = 0,
    val parent: Long = 0
)

@Serializable
data class PageDto(
    val id: Long = 0,
    val link: String = "",
    val title: RenderedField = RenderedField(),
    val content: RenderedField = RenderedField()
)
