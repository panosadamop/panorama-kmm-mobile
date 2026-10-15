package gr.panoramapolihnitou.app.data.local

import com.russhwolf.settings.Settings
import gr.panoramapolihnitou.app.data.remote.ApiJson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

/** A contributor unlocked with an access code. Roles come from the backend. */
@Serializable
data class Contributor(
    val code: String,
    val name: String,
    val roles: List<String>
) {
    val canSubmit: Boolean get() = ROLE_SUBMIT in roles
    val canStream: Boolean get() = ROLE_LIVE in roles

    companion object {
        const val ROLE_SUBMIT = "submit"
        const val ROLE_LIVE = "live"
    }
}

/**
 * Remembers the unlocked contributor so the code isn't re-typed on every launch.
 * The backend re-checks the code on every call, so revoking it server-side
 * locks the device out on its next action regardless of what's stored here.
 */
class ContributorSession(
    private val settings: Settings = Settings()
) {
    private val key = "contributor_v1"

    private val _current = MutableStateFlow(load())
    val current: StateFlow<Contributor?> = _current.asStateFlow()

    fun save(contributor: Contributor) {
        settings.putString(key, ApiJson.encodeToString(Contributor.serializer(), contributor))
        _current.value = contributor
    }

    fun clear() {
        settings.remove(key)
        _current.value = null
    }

    private fun load(): Contributor? {
        val raw = settings.getStringOrNull(key) ?: return null
        return runCatching { ApiJson.decodeFromString(Contributor.serializer(), raw) }.getOrNull()
    }
}
