package gr.panoramapolihnitou.app.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * The contributor area (password-gated uploads + live streaming) talks to the
 * "Πανόραμα — Συνεργάτες εφαρμογής" WordPress plugin on the site — see
 * `wordpress-integration/panorama-partners/README.md`. Partners and their
 * passwords, the newsroom mailbox and the YouTube stream key are all managed in
 * WordPress admin (Συνεργάτες), never in the app binary.
 */
object ContributorConfig {
    /**
     * The plugin's REST endpoint. Left empty, the contributor screens explain the
     * feature isn't set up yet instead of failing (same "missing config → skipped"
     * pattern as Firebase).
     */
    const val ENDPOINT_URL = "${ApiConfig.SITE_URL}/wp-json/panorama/v1/contributor"

    /** Shown in the destination picker; the plugin's "Email σύνταξης" setting is what's actually used. */
    const val MAIL_TO = "info@panoramapolihnitou.gr"

    /**
     * Mailboxes refuse attachments over ~25 MB and base64 inflates by a third, so
     * cap the raw total well under that. Measured: 18 MB fits PHP's default 128 MB
     * memory on the server; the plugin enforces its own 24 MB backstop.
     */
    const val MAX_UPLOAD_BYTES = 18L * 1024 * 1024

    val isConfigured: Boolean get() = ENDPOINT_URL.isNotBlank()
}

enum class SubmissionDestination(val wireValue: String) {
    /** Stored in WordPress admin → Συνεργάτες → Υλικό συνεργατών, files in the Media Library. */
    WORDPRESS("wordpress"),
    EMAIL("email")
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
        val response = client.post(endpointUrl) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        // The web server (not WordPress) refusing an oversized body answers with an HTML 413.
        if (response.status == HttpStatusCode.PayloadTooLarge) {
            throw ContributorException("too_large", errorMessage("too_large", null))
        }
        val parsed = runCatching { ApiJson.decodeFromString<ContributorResponse>(response.bodyAsText()) }
            .getOrElse { throw IllegalStateException("Μη αναμενόμενη απάντηση από τον διακομιστή.", it) }
        if (!parsed.ok) {
            val code = parsed.error ?: "server_error"
            throw ContributorException(code, errorMessage(code, parsed.message))
        }
        return parsed
    }

    private fun errorMessage(code: String, serverMessage: String?): String = when (code) {
        "invalid_code" -> "Ο κωδικός δεν είναι έγκυρος."
        "forbidden" -> "Ο κωδικός σας δεν έχει πρόσβαση σε αυτή τη λειτουργία."
        "too_large" -> "Τα συνημμένα είναι πολύ μεγάλα για αποστολή."
        "not_configured" -> "Η λειτουργία δεν έχει ρυθμιστεί ακόμη από τη συντακτική ομάδα."
        "rate_limited" -> "Πάρα πολλές λάθος προσπάθειες. Δοκιμάστε ξανά σε 15 λεπτά."
        else -> serverMessage ?: "Κάτι πήγε στραβά. Δοκιμάστε ξανά."
    }
}
