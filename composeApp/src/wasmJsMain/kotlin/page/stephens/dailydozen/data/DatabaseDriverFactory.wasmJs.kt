package page.stephens.dailydozen.data

import app.cash.sqldelight.async.coroutines.awaitCreate
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker
import page.stephens.dailydozen.db.DailyDozenDb

/**
 * Wasm driver. SQLDelight's WebWorkerDriver talks to a sql.js engine running in
 * a web worker (shipped by the @cashapp/sqldelight-sqljs-worker npm package).
 * Both driver creation and schema setup are async here, which is why
 * [create] — and the whole [DatabaseDriverFactory] API — is suspending.
 */
actual class DatabaseDriverFactory {
    actual suspend fun create(): SqlDriver {
        val driver = WebWorkerDriver(createWorker())
        DailyDozenDb.Schema.awaitCreate(driver)
        return driver
    }
}

/**
 * Builds the worker from the npm-published script. Expressed via [js] because
 * the URL is resolved by the bundler (import.meta.url), not at Kotlin level.
 */
private fun createWorker(): Worker =
    js("""new Worker(new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url))""")
