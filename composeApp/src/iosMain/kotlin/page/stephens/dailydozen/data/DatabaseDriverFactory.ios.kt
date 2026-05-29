package page.stephens.dailydozen.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import page.stephens.dailydozen.db.DailyDozenDb

/**
 * iOS driver. As on Android, the async-generated schema is adapted to a
 * synchronous schema for the synchronous NativeSqliteDriver.
 */
actual class DatabaseDriverFactory {
    actual suspend fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = DailyDozenDb.Schema.synchronous(),
            name = "dailydozen.db",
        )
}
