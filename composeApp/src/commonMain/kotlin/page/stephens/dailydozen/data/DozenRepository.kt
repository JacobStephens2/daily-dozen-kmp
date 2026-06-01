package page.stephens.dailydozen.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import page.stephens.dailydozen.db.DailyDozenDb
import page.stephens.dailydozen.domain.Categories
import page.stephens.dailydozen.domain.ServingLogic
import page.stephens.dailydozen.domain.model.DataPayload
import page.stephens.dailydozen.domain.model.LocalState
import page.stephens.dailydozen.domain.model.ProfileData

/**
 * Single source of truth for all app state. The whole [LocalState] is held in a
 * [StateFlow] in memory and persisted as one JSON blob via SQLDelight (see
 * `DailyDozen.sq`). User-driven mutations fire [onDataChanged] so the sync layer
 * can debounce a push; sync-driven updates (pull/replace) do not, to avoid loops.
 */
class DozenRepository(
    private val driverFactory: DatabaseDriverFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val mutex = Mutex()
    private var database: DailyDozenDb? = null
    private var loaded = false

    private val _state = MutableStateFlow(LocalState())
    val state: StateFlow<LocalState> = _state.asStateFlow()

    /** Set by the sync layer; invoked after any user-driven data change. */
    var onDataChanged: (() -> Unit)? = null

    private suspend fun db(): DailyDozenDb = mutex.withLock {
        database ?: DailyDozenDb(driverFactory.create()).also { database = it }
    }

    /** Load the persisted blob once. Safe to call repeatedly. */
    suspend fun ensureLoaded() = withContext(dispatcher) {
        if (loaded) return@withContext
        val stored = db().dailyDozenQueries.selectState().awaitAsOneOrNull()
        if (stored != null) {
            runCatching { json.decodeFromString<LocalState>(stored) }
                .onSuccess { _state.value = it }
        }
        loaded = true
    }

    private suspend fun persist(s: LocalState) = withContext(dispatcher) {
        db().dailyDozenQueries.upsertState(json.encodeToString(LocalState.serializer(), s))
    }

    private suspend fun update(notify: Boolean = true, transform: (LocalState) -> LocalState) {
        val next = transform(_state.value)
        if (next === _state.value) return
        _state.value = next
        persist(next)
        if (notify) onDataChanged?.invoke()
    }

    private fun LocalState.mutateProfile(
        profileId: String = currentProfile,
        block: (ProfileData) -> ProfileData,
    ): LocalState {
        val profiles = payload.profiles.toMutableMap()
        val current = profiles[profileId] ?: ProfileData(name = profileId, color = "#38672a")
        profiles[profileId] = block(current)
        return copy(payload = payload.copy(profiles = profiles))
    }

    // ---- Serving edits -------------------------------------------------------

    /** Apply the fill-to-left tap to [categoryId] serving [index] on [dateKey]. */
    suspend fun toggleServing(dateKey: String, categoryId: String, index: Int) = update {
        it.mutateProfile { p ->
            val servings = Categories.servingsFor(p.dietType, p.customServings)
            val target = servings[categoryId] ?: return@mutateProfile p
            val day = p.data[dateKey].orEmpty()
            val next = ServingLogic.toggle(day[categoryId].orEmpty().toSet(), target, index)
            val newDay = day.toMutableMap()
            if (next.isEmpty()) newDay.remove(categoryId) else newDay[categoryId] = next.sorted()
            val newData = p.data.toMutableMap()
            if (newDay.isEmpty()) newData.remove(dateKey) else newData[dateKey] = newDay
            p.copy(data = newData)
        }
    }

    /** Clear all entries for [dateKey] on the current profile. */
    suspend fun resetDay(dateKey: String) = update {
        val cleared = it.mutateProfile { p ->
            p.copy(data = p.data.toMutableMap().apply { remove(dateKey) })
        }
        cleared.copy(celebrations = cleared.celebrations - celebrationKey(it.currentProfile, dateKey))
    }

    // ---- Categories / presets ------------------------------------------------

    /** Switch to a named preset: records [presetId] and snapshots its servings. */
    suspend fun applyPreset(presetId: String) = update {
        val preset = Categories.presets[presetId] ?: return@update it
        it.mutateProfile { p -> p.copy(dietType = presetId, customServings = preset.servings) }
    }

    /** Set a custom per-category serving map for the current profile. */
    suspend fun setCustomServings(servings: Map<String, Int>) = update {
        it.mutateProfile { p -> p.copy(customServings = servings) }
    }

    // ---- Profiles ------------------------------------------------------------

    suspend fun switchProfile(profileId: String) =
        update(notify = false) { it.copy(currentProfile = profileId) }

    suspend fun renameProfile(profileId: String, name: String) = update {
        it.mutateProfile(profileId) { p -> p.copy(name = name) }
    }

    // ---- Celebration (local-only) -------------------------------------------

    fun celebrationKey(profileId: String, dateKey: String) = "$profileId|$dateKey"

    fun celebrationShown(dateKey: String): Boolean =
        _state.value.celebrations.contains(celebrationKey(_state.value.currentProfile, dateKey))

    suspend fun markCelebrationShown(dateKey: String) = update(notify = false) {
        it.copy(celebrations = it.celebrations + celebrationKey(it.currentProfile, dateKey))
    }

    // ---- Sync support --------------------------------------------------------

    fun currentPayload(): DataPayload = _state.value.payload

    /** Replace the synced payload (server pull / import). Does not notify. */
    suspend fun replacePayload(payload: DataPayload, lastSync: String?) = update(notify = false) {
        val cp = when {
            payload.profiles.containsKey(it.currentProfile) -> it.currentProfile
            else -> payload.profiles.keys.firstOrNull() ?: "user"
        }
        it.copy(payload = payload, currentProfile = cp, lastSync = lastSync ?: it.lastSync)
    }

    suspend fun setLastSync(ts: String?) = update(notify = false) { it.copy(lastSync = ts) }

    suspend fun setEmail(email: String?) = update(notify = false) { it.copy(email = email) }
}
