package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * Thin typed wrapper over the WordPress REST API v2 endpoints the app uses.
 * All requests are relative to the base URL configured in [applySharedConfig].
 */
class WordPressApi(private val client: HttpClient) {

    /**
     * Latest posts, newest first. [categoryId] filters by a single category;
     * [categoryIds] filters by a comma-separated set (used by the home sections);
     * [search] is an optional full-text filter.
     */
    suspend fun getPosts(
        page: Int = 1,
        perPage: Int = 10,
        categoryId: Long? = null,
        categoryIds: String? = null,
        search: String? = null
    ): List<PostDto> = client.get("posts") {
        parameter("_embed", "1")
        parameter("page", page)
        parameter("per_page", perPage)
        parameter("orderby", "date")
        parameter("order", "desc")
        categoryId?.let { parameter("categories", it) }
        categoryIds?.takeIf { it.isNotBlank() }?.let { parameter("categories", it) }
        search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
    }.body()

    /** Sticky (featured) posts for the home slider; falls back handled by repository. */
    suspend fun getStickyPosts(perPage: Int = 5): List<PostDto> = client.get("posts") {
        parameter("_embed", "1")
        parameter("sticky", "true")
        parameter("per_page", perPage)
    }.body()

    suspend fun getPost(id: Long): PostDto = client.get("posts/$id") {
        parameter("_embed", "1")
    }.body()

    suspend fun getCategories(perPage: Int = 100): List<CategoryDto> = client.get("categories") {
        parameter("per_page", perPage)
        parameter("hide_empty", "true")
        parameter("orderby", "count")
        parameter("order", "desc")
    }.body()

    suspend fun getPages(perPage: Int = 50): List<PageDto> = client.get("pages") {
        parameter("_embed", "1")
        parameter("per_page", perPage)
    }.body()

    suspend fun getPage(id: Long): PageDto = client.get("pages/$id") {
        parameter("_embed", "1")
    }.body()
}
