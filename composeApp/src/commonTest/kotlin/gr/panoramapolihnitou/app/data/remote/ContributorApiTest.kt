package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val ENDPOINT = "https://panoramapolihnitou.gr/wp-json/panorama/v1/contributor"
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json; charset=UTF-8")

/** Response bodies below are copied from the plugin running on WordPress 6.8 / PHP 8.3. */
class ContributorApiTest {

    private fun plugin(
        replyJson: String,
        status: HttpStatusCode = HttpStatusCode.OK,
        onPost: (HttpRequestData, String) -> Unit = { _, _ -> }
    ): ContributorApi {
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals(ENDPOINT, request.url.toString())
            onPost(request, request.body.toByteArray().decodeToString())
            respond(replyJson, status, jsonHeaders)
        }
        return ContributorApi(HttpClient(engine) { applyUploadConfig() }, ENDPOINT)
    }

    @Test
    fun endpointIsThePluginRoute() {
        assertEquals(ENDPOINT, ContributorConfig.ENDPOINT_URL)
    }

    @Test
    fun verifyReturnsNameAndRoles() = runTest {
        val api = plugin("""{"ok":true,"name":"Μαρία Π.","roles":["submit","live"]}""")
        val response = api.verify("Polichnitos#2026!")
        assertEquals("Μαρία Π.", response.name)
        assertEquals(listOf("submit", "live"), response.roles)
    }

    @Test
    fun invalidCodeIsReportedAsSuch() = runTest {
        val api = plugin("""{"ok":false,"error":"invalid_code"}""")
        val error = assertFailsWith<ContributorException> { api.verify("wrong-code-123") }
        assertTrue(error.isInvalidCode)
        assertEquals("Ο κωδικός δεν είναι έγκυρος.", error.message)
    }

    @Test
    fun throttlingHasItsOwnMessage() = runTest {
        val api = plugin("""{"ok":false,"error":"rate_limited"}""")
        val error = assertFailsWith<ContributorException> { api.verify("Polichnitos#2026!") }
        assertEquals("Πάρα πολλές λάθος προσπάθειες. Δοκιμάστε ξανά σε 15 λεπτά.", error.message)
    }

    @Test
    fun serverMessageIsShownForUnmappedErrors() = runTest {
        val api = plugin("""{"ok":false,"error":"server_error","message":"Η αποστολή email απέτυχε."}""")
        val error = assertFailsWith<ContributorException> { api.liveConfig("Polichnitos#2026!") }
        assertEquals("Η αποστολή email απέτυχε.", error.message)
    }

    @Test
    fun wordpressCoreErrorBecomesAGenericError() = runTest {
        // e.g. the plugin is deactivated: WordPress answers 404 rest_no_route, which has no "ok".
        val api = plugin(
            """{"code":"rest_no_route","message":"No route was found matching the URL and request method.","data":{"status":404}}""",
            HttpStatusCode.NotFound
        )
        val error = assertFailsWith<ContributorException> { api.verify("Polichnitos#2026!") }
        assertEquals("server_error", error.code)
    }

    @Test
    fun webServerBodyLimitIsReportedAsTooLarge() = runTest {
        val engine = MockEngine { respond("<html><body>413 Request Entity Too Large</body></html>", HttpStatusCode.PayloadTooLarge) }
        val api = ContributorApi(HttpClient(engine) { applyUploadConfig() }, ENDPOINT)
        val error = assertFailsWith<ContributorException> {
            api.submit("Polichnitos#2026!", SubmissionDestination.WORDPRESS, "t", "", emptyList())
        }
        assertEquals("too_large", error.code)
    }

    @Test
    fun htmlPageBecomesAReadableError() = runTest {
        // A security plugin or host error page in front of the REST API.
        val api = plugin("<html><body>Forbidden</body></html>")
        assertFailsWith<IllegalStateException> { api.verify("Polichnitos#2026!") }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun submitSendsBase64FilesAndTheDestination() = runTest {
        var sent = ""
        val api = plugin("""{"ok":true,"message":"Το υλικό παραδόθηκε στη σύνταξη."}""") { _, body -> sent = body }
        val response = api.submit(
            code = "giorgos-only-submit",
            destination = SubmissionDestination.WORDPRESS,
            title = "Τίτλος",
            text = "Κείμενο",
            files = listOf(SubmissionFile("photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
        )
        assertEquals("Το υλικό παραδόθηκε στη σύνταξη.", response.message)
        val json = ApiJson.parseToJsonElement(sent).jsonObject
        assertEquals("submit", json["action"]!!.jsonPrimitive.content)
        assertEquals("wordpress", json["destination"]!!.jsonPrimitive.content)
        val file = json["files"]!!.jsonArray.single().jsonObject
        assertEquals("photo.jpg", file["name"]!!.jsonPrimitive.content)
        assertEquals(Base64.encode(byteArrayOf(1, 2, 3)), file["data"]!!.jsonPrimitive.content)
    }

    @Test
    fun liveConfigCarriesTheStreamSettings() = runTest {
        val api = plugin("""{"ok":true,"rtmpUrl":"rtmps:\/\/a.rtmps.youtube.com\/live2","streamKey":"abcd-efgh-ijkl-mnop","watchUrl":null}""")
        val live = api.liveConfig("Polichnitos#2026!")
        assertEquals("rtmps://a.rtmps.youtube.com/live2", live.rtmpUrl)
        assertEquals("abcd-efgh-ijkl-mnop", live.streamKey)
        assertEquals(null, live.watchUrl)
    }
}
