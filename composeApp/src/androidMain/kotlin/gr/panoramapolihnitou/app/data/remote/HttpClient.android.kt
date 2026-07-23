package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

/** Android uses the OkHttp engine, sharing the common configuration. */
actual fun createHttpClient(): HttpClient = HttpClient(OkHttp) {
    applySharedConfig()
}
