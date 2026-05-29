package page.stephens.dailydozen.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import page.stephens.dailydozen.db.DailyDozenDb

/**
 * The single source of truth for logged servings, backed by SQLDelight.
 *
 * The database is opened lazily on first use (driver creation is `suspend`),
 * guarded by a [Mutex] so concurrent callers share one instance. Reads are
 * exposed as [Flow]s that re-emit whenever the underlying table changes, so the
 * UI updates reactively after every write.
 */
class DozenRepository(
    private val driverFactory: DatabaseDriverFactory,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val mutex = Mutex()
    private var database: DailyDozenDb? = null

    private suspend fun db(): DailyDozenDb = mutex.withLock {
        database ?: DailyDozenDb(driverFactory.create()).also { database = it }
    }

    /** Reactive map of categoryId -> logged count for [day] (yyyy-MM-dd). */
    fun countsForDay(day: String): Flow<Map<String, Int>> = flow {
        val queries = db().dailyDozenQueries
        emitAll(
            queries.selectForDay(day)
                .asFlow()
                .mapToList(dispatcher)
                .map { rows -> rows.associate { it.categoryId to it.count.toInt() } },
        )
    }

    /** Read-only history: every logged day (newest first) with its counts. */
    fun history(): Flow<List<DayHistory>> = flow {
        val queries = db().dailyDozenQueries
        emitAll(
            queries.selectHistory()
                .asFlow()
                .mapToList(dispatcher)
                .map { rows ->
                    rows.groupBy { it.day }
                        .map { (day, cells) ->
                            DayHistory(day, cells.associate { it.categoryId to it.count.toInt() })
                        }
                },
        )
    }

    /** Set the absolute serving [count] for a category on [day]. */
    suspend fun setCount(day: String, categoryId: String, count: Int) =
        withContext(dispatcher) {
            db().dailyDozenQueries.upsert(day, categoryId, count.toLong())
        }
}

/** A single past day's logged counts, keyed by categoryId. */
data class DayHistory(
    val day: String,
    val counts: Map<String, Int>,
)
