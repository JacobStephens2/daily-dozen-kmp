package page.stephens.dailydozen.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOneOrNull
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import page.stephens.dailydozen.data.model.DailyDozenJson
import page.stephens.dailydozen.data.model.Profile
import page.stephens.dailydozen.data.model.SyncBlob
import page.stephens.dailydozen.db.DailyDozenDb

/** Everything the checklist UI needs for one day, derived from the active profile. */
data class DayProgressInput(
    val dietType: String,
    val customServings: Map<String, Int>?,
    val counts: Map<String, Int>,
)

/** The persisted sync state: the blob plus the bookkeeping the sync engine needs. */
data class SyncState(
    val blob: SyncBlob,
    val syncedUpdatedAt: String?,
    val dirty: Boolean,
)

/**
 * Single source of truth for logged servings, now backed by the contract blob
 * (ADR-4) rather than the count schema. The blob is stored as one opaque JSON
 * text row, mirroring the server's opaque blob so the round-trip is lossless.
 *
 * The DB opens lazily behind a [Mutex]; on first open it runs the one-time,
 * non-destructive legacy `count -> indices` migration. Reads are reactive
 * [Flow]s so the UI updates after every write. The stepper UI's "N servings"
 * maps to the contract's checked-index set as `[0..N-1]`.
 */
class DozenRepository(
    private val driverFactory: DatabaseDriverFactory,
    private val json: Json = DailyDozenJson,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private var database: DailyDozenDb? = null

    /** v1 syncs a single active profile; multi-profile editing is out of scope. */
    val activeProfileId: String = "user"

    private suspend fun db(): DailyDozenDb = mutex.withLock {
        database ?: run {
            val db = DailyDozenDb(driverFactory.create())
            migrateLegacyIfNeeded(db)
            database = db
            db
        }
    }

    /** One-time, non-destructive migration: only runs when no blob exists yet. */
    private suspend fun migrateLegacyIfNeeded(db: DailyDozenDb) {
        val queries = db.dailyDozenQueries
        if (queries.selectState().awaitAsOneOrNull() != null) return
        val legacy = queries.selectAllLegacy().awaitAsList()
            .map { LegacyServing(it.day, it.categoryId, it.count.toInt()) }
        val blob = migrateCountsToBlob(legacy, activeProfileId)
        // dirty=true if we synthesized data the server hasn't seen; the old
        // servingLog rows are left in place until a verified first sync.
        queries.upsertState(json.encodeToString(SyncBlob.serializer(), blob), null, if (legacy.isEmpty()) 0L else 1L)
    }

    /** Reactive view of the whole blob; emits a fresh value after every write. */
    private fun blobFlow(): Flow<SyncBlob> = flow {
        val queries = db().dailyDozenQueries
        emitAll(
            queries.selectState().asFlow().mapToOneOrNull(dispatcher).map { row ->
                row?.blobJson?.let { json.decodeFromString(SyncBlob.serializer(), it) } ?: SyncBlob.empty()
            },
        )
    }

    /** Reactive checklist input for [day]: the active profile's diet + counts. */
    fun dayFlow(day: String): Flow<DayProgressInput> = blobFlow().map { blob ->
        val profile = blob.profiles[activeProfileId]
        DayProgressInput(
            dietType = profile?.dietType ?: "standard",
            customServings = profile?.customServings,
            counts = profile?.data?.get(day)?.mapValues { it.value.size } ?: emptyMap(),
        )
    }

    /**
     * Set the absolute serving [count] for a category on [day], represented as
     * the checked-index set `[0..count-1]`. Marks the store dirty for sync.
     */
    suspend fun setCount(day: String, categoryId: String, count: Int) = withContext(dispatcher) {
        updateBlob { blob ->
            val profile = blob.profiles[activeProfileId] ?: Profile()
            val newData = profile.data.toMutableMap()
            val dayMap = (newData[day] ?: emptyMap()).toMutableMap()
            if (count <= 0) dayMap.remove(categoryId) else dayMap[categoryId] = (0 until count).toList()
            if (dayMap.isEmpty()) newData.remove(day) else newData[day] = dayMap
            val newProfiles = blob.profiles.toMutableMap()
            newProfiles[activeProfileId] = profile.copy(data = newData)
            blob.copy(profiles = newProfiles)
        }
    }

    /** Read the full persisted state (for the sync engine, M4). */
    suspend fun loadState(): SyncState = withContext(dispatcher) {
        val row = db().dailyDozenQueries.selectState().awaitAsOneOrNull()
        SyncState(
            blob = row?.blobJson?.let { json.decodeFromString(SyncBlob.serializer(), it) } ?: SyncBlob.empty(),
            syncedUpdatedAt = row?.syncedUpdatedAt,
            dirty = (row?.dirty ?: 0L) != 0L,
        )
    }

    /** Persist [blob] with the given sync bookkeeping (used by the sync engine). */
    suspend fun writeState(blob: SyncBlob, syncedUpdatedAt: String?, dirty: Boolean) = withContext(dispatcher) {
        db().dailyDozenQueries.upsertState(
            json.encodeToString(SyncBlob.serializer(), blob),
            syncedUpdatedAt,
            if (dirty) 1L else 0L,
        )
    }

    private suspend fun updateBlob(transform: (SyncBlob) -> SyncBlob) {
        val queries = db().dailyDozenQueries
        val row = queries.selectState().awaitAsOneOrNull()
        val current = row?.blobJson?.let { json.decodeFromString(SyncBlob.serializer(), it) } ?: SyncBlob.empty()
        val updated = transform(current)
        // A local edit keeps the last-synced timestamp but flips dirty = true.
        queries.upsertState(json.encodeToString(SyncBlob.serializer(), updated), row?.syncedUpdatedAt, 1L)
    }
}
