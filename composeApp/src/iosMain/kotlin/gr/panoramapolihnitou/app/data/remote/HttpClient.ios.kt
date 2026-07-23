package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin

/** iOS uses the Darwin (NSURLSession) engine, sharing the common configuration. */
actual fun createHttpClient(): HttpClient = HttpClient(Darwin) {
    applySharedConfig()
}
