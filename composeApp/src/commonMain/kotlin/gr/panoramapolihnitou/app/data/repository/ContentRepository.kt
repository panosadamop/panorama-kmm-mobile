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
        // App front page only (the website's own front page is unaffected):
        // the slider shows just these categories, every other home section shows
        // everything except them, so no post appears twice.
        // 48 Νέα του χωριού, 49 Πολιχνίτος, 47 Στήλες & Απόψεις,
        // 55 Τοπική Αυτοδιοίκηση, 43 Λέσβος, 75 Πανόραμα
        private const val FEATURED_CATEGORY_IDS = "48,49,47,55,43,75"
        private const val IMPORTANT_CATEGORY_IDS = "44,46,61"  // Ελλάδα/Κόσμος, Εκδηλώσεις, Ιστορία
    }

    /**
     * Slider posts — strictly from [FEATURED_CATEGORY_IDS]. No fallback to other
     * categories: if this fails the slider is simply hidden.
     */
    suspend fun getFeatured(limit: Int = 6): List<Article> =
        runCatching { api.getPosts(perPage = limit, categoryIds = FEATURED_CATEGORY_IDS) }
            .getOrDefault(emptyList())
            .map { it.toArticle() }

    /** "Important" posts section on the home screen. */
    suspend fun getImportant(limit: Int = 4): List<Article> {
        val primary = runCatching {
            api.getPosts(
                perPage = limit,
                categoryIds = IMPORTANT_CATEGORY_IDS,
                excludeCategoryIds = FEATURED_CATEGORY_IDS
            )
        }.getOrDefault(emptyList())
        return primary.ifEmpty {
            runCatching {
                api.getPosts(page = 2, perPage = limit, excludeCategoryIds = FEATURED_CATEGORY_IDS)
            }.getOrDefault(emptyList())
        }.map { it.toArticle() }
    }

    /** "Latest news" feed on the home screen: every category except the slider's. */
    suspend fun getHomeLatest(page: Int, perPage: Int): List<Article> =
        api.getPosts(page = page, perPage = perPage, excludeCategoryIds = FEATURED_CATEGORY_IDS)
            .map { it.toArticle() }

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
