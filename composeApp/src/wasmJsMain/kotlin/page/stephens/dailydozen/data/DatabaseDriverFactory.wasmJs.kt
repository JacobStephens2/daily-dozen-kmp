package page.stephens.dailydozen.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Wasm driver placeholder. The real implementation uses SQLDelight's
 * WebWorkerDriver backed by a sql.js worker; it's wired in the next commit
 * once the npm/webpack plumbing is in place.
 */
actual class DatabaseDriverFactory {
    actual suspend fun create(): SqlDriver =
        TODO("Wasm web-worker SQLDelight driver — wired in the following commit")
}
