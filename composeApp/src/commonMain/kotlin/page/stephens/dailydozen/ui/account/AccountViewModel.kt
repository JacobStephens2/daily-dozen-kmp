package page.stephens.dailydozen.ui.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.data.auth.TokenStore
import page.stephens.dailydozen.data.model.DailyDozenJson
import page.stephens.dailydozen.data.model.SyncBlob
import page.stephens.dailydozen.data.remote.SyncApi
import page.stephens.dailydozen.data.sync.SyncEngine
import page.stephens.dailydozen.data.sync.SyncStatus

enum class AuthMode { LOGIN, REGISTER, FORGOT }

data class AccountUiState(
    val signedIn: Boolean = false,
    val email: String? = null,
    val mode: AuthMode = AuthMode.LOGIN,
    val loading: Boolean = false,
    val message: String? = null,
    /** When non-null, the UI shows the exported blob JSON for copy-out. */
    val exportJson: String? = null,
)

/**
 * Drives the account flow. The guiding rule (ADR-2 / §4): the account is
 * OPTIONAL — the app is local-first and fully usable signed out. Signing in only
 * adds sync. Logout and delete NEVER silently destroy local data.
 */
class AccountViewModel(
    private val api: SyncApi,
    private val tokenStore: TokenStore,
    private val syncEngine: SyncEngine,
    private val repository: DozenRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    val syncStatus: StateFlow<SyncStatus> = syncEngine.status

    init {
        viewModelScope.launch {
            if (tokenStore.read() != null) {
                _state.update { it.copy(signedIn = true) }
                runCatching { syncEngine.onSignedIn() }
            }
        }
    }

    fun setMode(mode: AuthMode) = _state.update { it.copy(mode = mode, message = null) }

    fun login(email: String, password: String) = launchGuarded {
        api.login(email.trim(), password)
        _state.update { it.copy(signedIn = true, email = email.trim()) }
        syncEngine.onSignedIn()
    }

    fun register(email: String, password: String) = launchGuarded {
        api.register(email.trim(), password)
        _state.update { it.copy(signedIn = true, email = email.trim()) }
        syncEngine.onSignedIn()
    }

    fun forgotPassword(email: String) = launchGuarded {
        api.forgotPassword(email.trim())
        _state.update { it.copy(mode = AuthMode.LOGIN, message = "If that email exists, a reset link is on its way.") }
    }

    /**
     * Logout = flush pending changes (best-effort), then clear ONLY the token.
     * Local data is left completely intact (the inversion of the Chart35 outage).
     */
    fun logout() = viewModelScope.launch {
        _state.update { it.copy(loading = true, message = "Saving your changes before signing out…") }
        runCatching { syncEngine.syncNow() } // best-effort; offline failure must not block logout or lose data
        tokenStore.clear() // the ONLY place a token is cleared
        _state.update {
            AccountUiState(signedIn = false, message = "Signed out. Your logged days remain on this device.")
        }
    }

    /** Data portability (§4): expose the exact blob JSON for the user to copy out. */
    fun exportData() = viewModelScope.launch {
        val blob = repository.loadState().blob
        _state.update { it.copy(exportJson = DailyDozenJson.encodeToString(SyncBlob.serializer(), blob)) }
    }

    fun dismissExport() = _state.update { it.copy(exportJson = null) }

    /**
     * Local-only deletion, labeled honestly: the backend exposes no delete
     * endpoint (§2), so we never imply a server-side wipe (Claude's data-dignity
     * finding). This clears the on-device blob only.
     */
    fun deleteLocalData() = viewModelScope.launch {
        repository.writeState(SyncBlob.empty(), syncedUpdatedAt = null, dirty = false)
        _state.update {
            it.copy(message = "Local data deleted on this device only. Synced data on the server is unaffected.")
        }
    }

    fun acknowledgeRemoteDeletionNotice() = syncEngine.acknowledgeRemoteDeletionNotice()

    fun clearMessage() = _state.update { it.copy(message = null) }

    private fun launchGuarded(block: suspend () -> Unit) = viewModelScope.launch {
        _state.update { it.copy(loading = true, message = null) }
        try {
            block()
            _state.update { it.copy(loading = false) }
        } catch (e: Throwable) {
            _state.update { it.copy(loading = false, message = e.message ?: "Something went wrong.") }
        }
    }
}
