package page.stephens.bountywell.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import page.stephens.bountywell.db.BountywellDb

/**
 * Android driver. The schema is async-generated (see build.gradle.kts), so it's
 * adapted to a synchronous schema for the synchronous AndroidSqliteDriver.
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual suspend fun create(): SqlDriver =
        AndroidSqliteDriver(
            schema = BountywellDb.Schema.synchronous(),
            context = context,
            // Legacy on-device filename, kept so existing installs keep their data.
            name = "dailydozen.db",
        )
}
