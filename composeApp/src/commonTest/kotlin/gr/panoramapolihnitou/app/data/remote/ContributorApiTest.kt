package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
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

private const val EXEC_URL = "https://script.google.com/macros/s/TEST/exec"
private const val ECHO_URL = "https://script.googleusercontent.com/macros/echo?user_content_key=abc"
private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

class ContributorApiTest {

    /** Mimics Apps Script: POST /exec → 302 → GET the echo URL for the real body. */
    private fun appsScript(
        replyJson: String,
        onPost: (HttpRequestData, String) -> Unit = { _, _ -> }
    ): ContributorApi {
        val engine = MockEngine { request ->
            when (request.method) {
                HttpMethod.Post -> {
                    onPost(request, request.body.toByteArray().decodeToString())
                    respond("", HttpStatusCode.Found, headersOf(HttpHeaders.Location, ECHO_URL))
                }
                else -> respondEcho(request, replyJson)
            }
        }
        return ContributorApi(HttpClient(engine) { applyUploadConfig() }, EXEC_URL)
    }

    private fun MockRequestHandleScope.respondEcho(request: HttpRequestData, body: String) =
        if (request.url.toString() == ECHO_URL) respond(body, HttpStatusCode.OK, jsonHeaders)
        else respond("unexpected ${request.url}", HttpStatusCode.NotFound)

    @Test
    fun verifyFollowsTheAppsScriptRedirect() = runTest {
        val api = appsScript("""{"ok":true,"name":"Μαρία","roles":["submit","live"]}""")
        val response = api.verify("k7Qe-29fz-Lm4X")
        assertEquals("Μαρία", response.name)
        assertEquals(listOf("submit", "live"), response.roles)
    }

    @Test
    fun directJsonResponseWorksToo() = runTest {
        // NSURLSession (iOS) may follow the redirect itself and hand back the final 200.
        val engine = MockEngine { respond("""{"ok":true,"name":"Γ","roles":[]}""", HttpStatusCode.OK, jsonHeaders) }
        val api = ContributorApi(HttpClient(engine) { applyUploadConfig() }, EXEC_URL)
        assertEquals("Γ", api.verify("pX3n-8wRa-Tt5c").name)
    }

    @Test
    fun invalidCodeIsReportedAsSuch() = runTest {
        val api = appsScript("""{"ok":false,"error":"invalid_code"}""")
        val error = assertFailsWith<ContributorException> { api.verify("wrong-code") }
        assertTrue(error.isInvalidCode)
        assertEquals("Ο κωδικός δεν είναι έγκυρος.", error.message)
    }

    @Test
    fun htmlErrorPageBecomesAReadableError() = runTest {
        // A wrongly deployed script ("Only myself" access) answers with Google's HTML sign-in page.
        val api = appsScript("<html><body>Sign in</body></html>")
        assertFailsWith<IllegalStateException> { api.verify("k7Qe-29fz-Lm4X") }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun submitSendsBase64FilesAndTheDestination() = runTest {
        var sent = ""
        val api = appsScript("""{"ok":true,"message":"ok"}""") { _, body -> sent = body }
        api.submit(
            code = "k7Qe-29fz-Lm4X",
            destination = SubmissionDestination.DRIVE,
            title = "Τίτλος",
            text = "Κείμενο",
            files = listOf(SubmissionFile("photo.jpg", "image/jpeg", byteArrayOf(1, 2, 3)))
        )
        val json = ApiJson.parseToJsonElement(sent).jsonObject
        assertEquals("submit", json["action"]!!.jsonPrimitive.content)
        assertEquals("drive", json["destination"]!!.jsonPrimitive.content)
        val file = json["files"]!!.jsonArray.single().jsonObject
        assertEquals("photo.jpg", file["name"]!!.jsonPrimitive.content)
        assertEquals(Base64.encode(byteArrayOf(1, 2, 3)), file["data"]!!.jsonPrimitive.content)
    }
}
