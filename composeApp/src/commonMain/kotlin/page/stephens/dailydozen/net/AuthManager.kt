package page.stephens.dailydozen.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.Clock
import page.stephens.dailydozen.data.DozenRepository

/**
 * Owns the auth token and orchestrates cross-device sync against [ApiClient],
 * persisting profile data through [DozenRepository]. Mirrors the web app's
 * debounced push + timestamp-based pull/push conflict resolution.
 */
class AuthManager(
    private val api: ApiClient,
    private val tokenStore: SecureTokenStore,
    private val repository: DozenRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _token = MutableStateFlow(tokenStore.getToken())
    private val _isLoggedIn = MutableStateFlow(_token.value != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private var pushJob: Job? = null

    init {
        // After any user-driven change, debounce a push to the server.
        repository.onDataChanged = { schedulePush() }
    }

    private fun saveToken(token: String?) {
        tokenStore.setToken(token)
        _token.value = token
        _isLoggedIn.value = token != null
    }

    private fun requireToken(): String =
        _token.value ?: throw ApiException(401, "Not signed in")

    suspend fun register(email: String, password: String) {
        val result = api.register(email, password)
        saveToken(result.token)
        repository.setEmail(result.email)
        pushData()
    }

    suspend fun login(email: String, password: String) {
        val result = api.login(email, password)
        saveToken(result.token)
        repository.setEmail(result.email)
        pullData()
    }

    suspend fun logout() {
        saveToken(null)
        repository.setEmail(null)
        repository.setLastSync(null)
    }

    suspend fun forgotPassword(email: String): String =
        api.forgotPassword(email).message ?: "If an account exists, a reset link has been sent."

    suspend fun resetPassword(token: String, password: String): String =
        api.resetPassword(token, password).message ?: "Password has been reset."

    /** Push local data to the server. */
    suspend fun pushData() {
        val token = _token.value ?: return
        val result = api.putData(token, repository.currentPayload())
        result.updatedAt?.let { repository.setLastSync(it) }
    }

    /** Pull server data and replace local. */
    private suspend fun pullData() {
        val token = _token.value ?: return
        val result = api.getData(token)
        if (result.data != null) {
            repository.replacePayload(result.data, result.updatedAt)
        }
    }

    /** Two-way sync: pull if the server copy is newer, else push. */
    suspend fun syncNow() {
        val token = _token.value ?: return
        val server = api.getData(token)
        val localTs = repository.state.value.lastSync
        val serverTs = server.updatedAt
        if (server.data != null && serverTs != null && (localTs == null || serverTs > localTs)) {
            repository.replacePayload(server.data, serverTs)
        } else {
            pushData()
        }
    }

    /** Debounced background push after a local change (3s, matching the web app). */
    private fun schedulePush() {
        if (_token.value == null) return
        pushJob?.cancel()
        pushJob = scope.launch {
            delay(3_000)
            runCatching { pushData() }
        }
    }

    /** On launch, refresh a near-expiry token and sync if signed in. */
    fun startupSync() {
        if (_token.value == null) return
        scope.launch {
            refreshTokenIfNeeded()
            runCatching { syncNow() }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private suspend fun refreshTokenIfNeeded() {
        val token = _token.value ?: return
        val expMillis = runCatching {
            val payload = token.split(".")[1]
            val padded = payload.padEnd((payload.length + 3) / 4 * 4, '=')
            val bytes = Base64.UrlSafe.decode(padded)
            val exp = Json.parseToJsonElement(bytes.decodeToString())
                .jsonObject["exp"]!!.jsonPrimitive.long
            exp * 1000
        }.getOrNull() ?: return
        val expiresIn = expMillis - Clock.System.now().toEpochMilliseconds()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
        if (expiresIn in 1..sevenDaysMs) {
            runCatching {
                val refreshed = api.refreshToken(token)
                saveToken(refreshed.token)
                repository.setEmail(refreshed.email)
            }
        }
    }
}
