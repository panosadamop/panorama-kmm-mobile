package gr.panoramapolihnitou.app.ui.screens.contributor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import gr.panoramapolihnitou.app.data.local.ContributorSession
import gr.panoramapolihnitou.app.data.remote.ContributorApi
import gr.panoramapolihnitou.app.data.remote.ContributorConfig
import gr.panoramapolihnitou.app.data.remote.ContributorException
import gr.panoramapolihnitou.app.data.remote.SubmissionDestination
import gr.panoramapolihnitou.app.data.remote.SubmissionFile
import gr.panoramapolihnitou.app.media.MediaAttachment
import kotlinx.coroutines.launch

/** Form state lives in Compose state: it's edited keystroke by keystroke from the UI. */
class SubmissionScreenModel(
    private val api: ContributorApi,
    private val session: ContributorSession
) : ScreenModel {

    var title by mutableStateOf("")
    var text by mutableStateOf("")
    var destination by mutableStateOf(SubmissionDestination.WORDPRESS)
    val attachments = mutableStateListOf<MediaAttachment>()

    var sending by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
    var successMessage by mutableStateOf<String?>(null)
        private set

    val totalBytes: Long get() = attachments.sumOf { it.bytes.size.toLong() }
    val overLimit: Boolean get() = totalBytes > ContributorConfig.MAX_UPLOAD_BYTES
    val canSend: Boolean
        get() = !sending && !overLimit && (text.isNotBlank() || attachments.isNotEmpty())

    fun add(items: List<MediaAttachment>) {
        attachments.addAll(items)
        error = null
    }

    fun remove(item: MediaAttachment) {
        attachments.remove(item)
    }

    fun send() {
        val contributor = session.current.value ?: return
        if (!canSend) return
        sending = true
        error = null
        screenModelScope.launch {
            runCatching {
                api.submit(
                    code = contributor.code,
                    destination = destination,
                    title = title.trim(),
                    text = text.trim(),
                    files = attachments.map { SubmissionFile(it.fileName, it.mimeType, it.bytes) }
                )
            }.onSuccess {
                successMessage = it.message ?: "Το υλικό στάλθηκε. Ευχαριστούμε!"
            }.onFailure {
                error = it.contributorMessage()
                if (it is ContributorException && it.isInvalidCode) session.clear()
            }
            sending = false
        }
    }

    /** Called when the success dialog is dismissed — ready for the next report. */
    fun reset() {
        title = ""
        text = ""
        attachments.clear()
        successMessage = null
        error = null
    }
}
