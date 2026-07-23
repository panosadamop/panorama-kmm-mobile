package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** REST base host + path prefix, from the specification. */
object ApiConfig {
    const val HOST = "panoramapolihnitou.gr"
    const val BASE_PATH = "/wp-json/wp/v2/"
    const val SITE_URL = "https://panoramapolihnitou.gr"
}

/** Lenient JSON: WordPress payloads carry many fields the app does not model. */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    coerceInputValues = true
}

/**
 * Shared client configuration, applied identically on every platform.
 * Actual [createHttpClient] implementations call this with their engine.
 */
fun HttpClientConfig<*>.applySharedConfig() {
    install(ContentNegotiation) {
        json(ApiJson)
    }
    install(Logging) {
        level = LogLevel.INFO
    }
    install(HttpTimeout) {
        requestTimeoutMillis = 30_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 30_000
    }
    defaultRequest {
        url {
            protocol = URLProtocol.HTTPS
            host = ApiConfig.HOST
            path(ApiConfig.BASE_PATH)
        }
    }
}

/**
 * Each platform supplies its own engine (OkHttp on Android, Darwin on iOS) but
 * reuses [applySharedConfig]. Implemented in androidMain / iosMain.
 */
expect fun createHttpClient(): HttpClient
