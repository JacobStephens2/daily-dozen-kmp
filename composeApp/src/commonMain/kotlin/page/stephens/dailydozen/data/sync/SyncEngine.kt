package page.stephens.dailydozen.data.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import page.stephens.dailydozen.data.DozenRepository
import page.stephens.dailydozen.data.auth.TokenStore
import page.stephens.dailydozen.data.model.DailyDozenJson
import page.stephens.dailydozen.data.model.SyncBlob
import page.stephens.dailydozen.data.remote.NeedsReauthException
import page.stephens.dailydozen.data.remote.SyncApi

/** What the UI shows about sync health — the antidote to silent failure (§4). */
data class SyncStatus(
    val signedIn: Boolean = false,
    val syncing: Boolean = false,
    val lastSyncedAt: String? = null,
    val offline: Boolean = false,
    val needsReauth: Boolean = false,
    val remoteDeletionsSuppressed: Boolean = false,
)

/**
 * Orchestrates the sync round: GET server -> [decideSync] -> adopt / push / merge
 * -> persist. All data-loss mitigations live in [decideSync]; this class only
 * does IO and never wipes local data. A failed refresh (NeedsReauth) and any
 * network error both keep local data intact and just update status.
 *
 * Local edits call [requestSync] which debounces 3 s (§7) before syncing.
 */
class SyncEngine(
    private val api: SyncApi,
    private val repository: DozenRepository,
    private val tokenStore: TokenStore,
    private val json: Json = DailyDozenJson,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status

    private val syncMutex = Mutex()
    private var debounceJob: Job? = null

    /** Debounced sync after a local edit (§7's 3 s coalescing window). */
    fun requestSync(debounceMs: Long = 3_000) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(debounceMs)
            syncNow()
        }
    }

    /** Run one sync round now. Returns true on a successful round-trip. */
    suspend fun syncNow(): Boolean = syncMutex.withLock {
        if (tokenStore.read() == null) {
            _status.update { it.copy(signedIn = false, syncing = false) }
            return false
        }
        _status.update { it.copy(signedIn = true, syncing = true) }
        try {
            val local = repository.loadState()
            val envelope = api.getData()
            val serverBlob = envelope.data?.let { json.decodeFromJsonElement(SyncBlob.serializer(), it) }

            var suppressed = false
            when (val decision = decideSync(local, serverBlob, envelope.updatedAt)) {
                is SyncDecision.Noop -> Unit
                is SyncDecision.Adopt ->
                    repository.writeState(decision.server, decision.updatedAt, dirty = false)
                is SyncDecision.Push -> {
                    val updatedAt = api.putData(json.encodeToJsonElement(SyncBlob.serializer(), decision.blob))
                    repository.writeState(decision.blob, updatedAt, dirty = false)
                }
                is SyncDecision.Merge -> {
                    suppressed = decision.remoteDeletionsSuppressed
                    val updatedAt = api.putData(json.encodeToJsonElement(SyncBlob.serializer(), decision.merged))
                    repository.writeState(decision.merged, updatedAt, dirty = false)
                }
            }
            _status.update {
                it.copy(
                    signedIn = true,
                    syncing = false,
                    offline = false,
                    needsReauth = false,
                    lastSyncedAt = repository.loadState().syncedUpdatedAt,
                    remoteDeletionsSuppressed = it.remoteDeletionsSuppressed || suppressed,
                )
            }
            return true
        } catch (e: NeedsReauthException) {
            // Refresh already failed. Keep ALL local data; just ask for re-auth (D1).
            _status.update { it.copy(syncing = false, needsReauth = true) }
            return false
        } catch (e: Throwable) {
            // Offline or transient: keep local intact, surface offline state (never wipe).
            _status.update { it.copy(syncing = false, offline = true) }
            return false
        }
    }

    /** Call after login: pull-and-adopt on a fresh device, merge on an existing one. */
    suspend fun onSignedIn(): Boolean {
        _status.update { it.copy(signedIn = true) }
        return syncNow()
    }

    fun acknowledgeRemoteDeletionNotice() {
        _status.update { it.copy(remoteDeletionsSuppressed = false) }
    }
}
