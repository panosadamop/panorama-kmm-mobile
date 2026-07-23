package gr.panoramapolihnitou.app.di

import gr.panoramapolihnitou.app.data.local.BookmarkStore
import gr.panoramapolihnitou.app.data.local.PreferencesStore
import gr.panoramapolihnitou.app.data.remote.WordPressApi
import gr.panoramapolihnitou.app.data.remote.createHttpClient
import gr.panoramapolihnitou.app.data.repository.ContentRepository

/**
 * Minimal, explicit dependency container. Everything is a lazily-created
 * singleton for the app's lifetime — no reflection, no DI framework, easy to
 * follow. Screen models pull what they need from here.
 */
object AppGraph {
    private val httpClient by lazy { createHttpClient() }
    private val api by lazy { WordPressApi(httpClient) }

    val repository: ContentRepository by lazy { ContentRepository(api) }
    val bookmarkStore: BookmarkStore by lazy { BookmarkStore() }
    val preferences: PreferencesStore by lazy { PreferencesStore() }
}
