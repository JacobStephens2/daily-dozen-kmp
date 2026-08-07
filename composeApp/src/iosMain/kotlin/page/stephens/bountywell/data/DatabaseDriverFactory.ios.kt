package page.stephens.bountywell.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import page.stephens.bountywell.db.BountywellDb

/**
 * iOS driver. As on Android, the async-generated schema is adapted to a
 * synchronous schema for the synchronous NativeSqliteDriver.
 */
actual class DatabaseDriverFactory {
    actual suspend fun create(): SqlDriver =
        NativeSqliteDriver(
            schema = BountywellDb.Schema.synchronous(),
            // Legacy on-device filename, kept so existing installs keep their data.
            name = "dailydozen.db",
        )
}
