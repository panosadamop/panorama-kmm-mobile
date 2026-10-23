package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The contributor area (password-gated uploads + live streaming) talks to a
 * Google Apps Script web app — see `backend/apps-script/README.md`. Access codes,
 * the Drive folder, the destination mailbox and the YouTube stream key all live
 * in that script's properties, never in the app binary.
 */
object ContributorConfig {
    /**
     * The web app's `/exec` URL from Apps Script → Deploy → Manage deployments.
     * Left empty, the contributor screens explain the feature isn't set up yet
     * instead of failing (same "missing config → skipped" pattern as Firebase).
     */
    const val ENDPOINT_URL =
        "https://script.google.com/macros/s/AKfycbyQW7EvzSLB7SV-kel9mfBjUySBMBHvc7zCUfQTI3SExvZsBbcvengXB4hLQVBhV61bkQ/exec"

    /** Shown in the destination picker; the script's MAIL_TO property is what's actually used. */
    const val MAIL_TO = "info@panoramapolihnitou.gr"

    /**
     * Apps Script rejects request bodies much over ~50 MB and Gmail attachments
     * over 25 MB; base64 inflates by a third, so cap the raw total well under both.
     */
    const val MAX_UPLOAD_BYTES = 18L * 1024 * 1024

    val isConfigured: Boolean get() = ENDPOINT_URL.isNotBlank()
}

enum class SubmissionDestination(val wireValue: String) {
    EMAIL("email"),
    DRIVE("drive")
}

class SubmissionFile(val name: String, val mimeType: String, val bytes: ByteArray)

@Serializable
data class ContributorResponse(
    val ok: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val name: String? = null,
    val roles: List<String> = emptyList(),
    val rtmpUrl: String? = null,
    val streamKey: String? = null,
    val watchUrl: String? = null
)

/** The backend answered but refused — e.g. a wrong/revoked code or a missing role. */
class ContributorException(val code: String, message: String) : Exception(message) {
    val isInvalidCode: Boolean get() = code == "invalid_code"
}

@Serializable
private data class ContributorRequest(
    val action: String,
    val code: String,
    val destination: String? = null,
    val title: String? = null,
    val text: String? = null,
    val files: List<FilePayload>? = null
)

@Serializable
private data class FilePayload(
    val name: String,
    val mimeType: String,
    @SerialName("data") val base64: String
)

class ContributorApi(
    private val client: HttpClient,
    private val endpointUrl: String = ContributorConfig.ENDPOINT_URL
) {

    suspend fun verify(code: String): ContributorResponse =
        call(ContributorRequest(action = "verify", code = code))

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun submit(
        code: String,
        destination: SubmissionDestination,
        title: String,
        text: String,
        files: List<SubmissionFile>
    ): ContributorResponse = call(
        ContributorRequest(
            action = "submit",
            code = code,
            destination = destination.wireValue,
            title = title,
            text = text,
            files = files.map { FilePayload(it.name, it.mimeType, Base64.encode(it.bytes)) }
        )
    )

    suspend fun liveConfig(code: String): ContributorResponse =
        call(ContributorRequest(action = "live", code = code))

    private suspend fun call(request: ContributorRequest): ContributorResponse {
        val first = client.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        val response = followAppsScriptRedirect(first)
        val parsed = runCatching { ApiJson.decodeFromString<ContributorResponse>(response.bodyAsText()) }
            .getOrElse { throw IllegalStateException("Μη αναμενόμενη απάντηση από τον διακομιστή.", it) }
        if (!parsed.ok) {
            val code = parsed.error ?: "server_error"
            throw ContributorException(code, errorMessage(code, parsed.message))
        }
        return parsed
    }

    /**
     * Apps Script answers every POST with a 302 to a one-off
     * script.googleusercontent.com URL that holds the real response and must be
     * fetched with GET. Ktor only auto-follows redirects for GET/HEAD, so do it here.
     */
    private suspend fun followAppsScriptRedirect(response: HttpResponse): HttpResponse {
        if (response.status.value !in 300..399) return response
        val location = response.headers[HttpHeaders.Location] ?: return response
        return client.get(location)
    }

    private fun errorMessage(code: String, serverMessage: String?): String = when (code) {
        "invalid_code" -> "Ο κωδικός δεν είναι έγκυρος."
        "forbidden" -> "Ο κωδικός σας δεν έχει πρόσβαση σε αυτή τη λειτουργία."
        "too_large" -> "Τα συνημμένα είναι πολύ μεγάλα για αποστολή."
        "not_configured" -> "Η λειτουργία δεν έχει ρυθμιστεί ακόμη από τη συντακτική ομάδα."
        else -> serverMessage ?: "Κάτι πήγε στραβά. Δοκιμάστε ξανά."
    }
}
