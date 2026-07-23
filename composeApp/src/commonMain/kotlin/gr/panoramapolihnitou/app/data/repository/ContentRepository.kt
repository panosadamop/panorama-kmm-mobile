package gr.panoramapolihnitou.app.data.repository

import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.data.model.Category
import gr.panoramapolihnitou.app.data.model.StaticPage
import gr.panoramapolihnitou.app.data.remote.WordPressApi
import gr.panoramapolihnitou.app.data.remote.toArticle
import gr.panoramapolihnitou.app.data.remote.toCategory
import gr.panoramapolihnitou.app.data.remote.toStaticPage

/**
 * Single source of truth for remote content. Maps DTOs to domain models and
 * applies small in-memory caches so tab switches don't re-hit the network.
 */
class ContentRepository(private val api: WordPressApi) {

    private var categoriesCache: List<Category>? = null
    private var pagesCache: List<StaticPage>? = null

    companion object {
        // Top categories on the live site (mirrors the reference app's wordpress.js).
        private const val FEATURED_CATEGORY_IDS = "49,53,43"   // Πολιχνίτος, Επικαιρότητα, Λέσβος
        private const val IMPORTANT_CATEGORY_IDS = "44,46,61"  // Ελλάδα/Κόσμος, Εκδηλώσεις, Ιστορία
    }

    /** Featured posts for the slider (top categories); falls back to sticky posts. */
    suspend fun getFeatured(limit: Int = 6): List<Article> {
        val primary = runCatching {
            api.getPosts(perPage = limit, categoryIds = FEATURED_CATEGORY_IDS)
        }.getOrDefault(emptyList())
        val source = primary.ifEmpty {
            runCatching { api.getStickyPosts(limit) }.getOrDefault(emptyList())
        }.ifEmpty { api.getPosts(page = 1, perPage = limit) }
        return source.map { it.toArticle() }
    }

    /** "Important" posts section on the home screen. */
    suspend fun getImportant(limit: Int = 4): List<Article> {
        val primary = runCatching {
            api.getPosts(perPage = limit, categoryIds = IMPORTANT_CATEGORY_IDS)
        }.getOrDefault(emptyList())
        return primary.ifEmpty {
            runCatching { api.getPosts(page = 2, perPage = limit) }.getOrDefault(emptyList())
        }.map { it.toArticle() }
    }

    suspend fun getPosts(
        page: Int = 1,
        perPage: Int = 10,
        categoryId: Long? = null,
        search: String? = null
    ): List<Article> =
        api.getPosts(page = page, perPage = perPage, categoryId = categoryId, search = search)
            .map { it.toArticle() }

    suspend fun getArticle(id: Long): Article = api.getPost(id).toArticle()

    suspend fun getCategories(forceRefresh: Boolean = false): List<Category> {
        categoriesCache?.takeIf { !forceRefresh }?.let { return it }
        return api.getCategories()
            .map { it.toCategory() }
            .filter { it.count > 0 }
            .also { categoriesCache = it }
    }

    suspend fun getPages(forceRefresh: Boolean = false): List<StaticPage> {
        pagesCache?.takeIf { !forceRefresh }?.let { return it }
        return api.getPages().map { it.toStaticPage() }.also { pagesCache = it }
    }

    suspend fun getPage(id: Long): StaticPage = api.getPage(id).toStaticPage()
}
