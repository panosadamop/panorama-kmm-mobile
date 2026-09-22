package gr.panoramapolihnitou.app.data.local

import coil3.PlatformContext
import gr.panoramapolihnitou.app.data.model.Article
import gr.panoramapolihnitou.app.di.AppGraph
import gr.panoramapolihnitou.app.util.ContentBlock
import gr.panoramapolihnitou.app.util.bookmarkImageCacheDir
import gr.panoramapolihnitou.app.util.parseHtmlToBlocks
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path

/**
 * Mirrors bookmarked articles' images (featured image + any inline images in
 * the body) to [bookmarkImageCacheDir] so a bookmark stays fully readable with
 * no network at all. [NetworkImage] prefers a hit here over a live request.
 * [sync] is called whenever the bookmark set changes and keeps the directory in
 * step with it: new images are downloaded, images for removed bookmarks are deleted.
 */
object BookmarkImageCache {
    private val fileSystem = FileSystem.SYSTEM

    suspend fun sync(context: PlatformContext, bookmarks: List<Article>) = withContext(Dispatchers.Default) {
        val dir = bookmarkImageCacheDir(context)
        val dirReady = runCatching { fileSystem.createDirectories(dir) }.isSuccess
        if (!dirReady) return@withContext

        val wanted = bookmarks.flatMap { imageUrls(it) }.toSet()
            .associateWith { dir.resolve(fileNameFor(it)) }

        wanted.forEach { (url, path) ->
            if (!fileSystem.exists(path)) downloadTo(url, path)
        }

        val keep = wanted.values.map { it.name }.toSet()
        runCatching { fileSystem.list(dir) }.getOrDefault(emptyList()).forEach { existing ->
            if (existing.name !in keep) runCatching { fileSystem.delete(existing) }
        }
    }

    /** The on-disk copy of [url], if it's already been cached for a bookmark. */
    fun cachedFileOrNull(context: PlatformContext, url: String): Path? {
        val path = bookmarkImageCacheDir(context).resolve(fileNameFor(url))
        return path.takeIf { fileSystem.exists(it) }
    }

    private fun imageUrls(article: Article): List<String> {
        val inline = parseHtmlToBlocks(article.contentHtml)
            .filterIsInstance<ContentBlock.Image>()
            .map { it.url }
        return (listOfNotNull(article.imageUrl) + inline).distinct()
    }

    private suspend fun downloadTo(url: String, path: Path) {
        runCatching {
            val bytes = AppGraph.httpClient.get(url).body<ByteArray>()
            fileSystem.write(path) { write(bytes) }
        }
    }

    // A stable, filesystem-safe name derived from the URL (not the OS String.hashCode(),
    // which Kotlin doesn't guarantee identically across platforms).
    private fun fileNameFor(url: String): String {
        var hash = -3750763034362895579L // FNV-1a 64-bit offset basis
        for (byte in url.encodeToByteArray()) {
            hash = hash xor (byte.toLong() and 0xff)
            hash *= 1099511628211L // FNV-1a 64-bit prime
        }
        return hash.toULong().toString(16)
    }
}
