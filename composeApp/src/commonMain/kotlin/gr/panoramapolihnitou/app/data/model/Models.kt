package gr.panoramapolihnitou.app.data.model

import kotlinx.serialization.Serializable

/**
 * Domain models used by the UI layer. These are deliberately decoupled from the
 * WordPress REST DTOs so the app is resilient to backend field changes.
 */

@Serializable
data class Article(
    val id: Long,
    val title: String,
    val excerptHtml: String,
    val contentHtml: String,
    val dateIso: String,
    val link: String,
    val imageUrl: String?,
    val categoryIds: List<Long>,
    val categoryNames: List<String>,
    val tagNames: List<String> = emptyList()
)

@Serializable
data class Category(
    val id: Long,
    val name: String,
    val count: Int,
    val parent: Long
)

@Serializable
data class StaticPage(
    val id: Long,
    val title: String,
    val contentHtml: String,
    val link: String
)
