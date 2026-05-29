package page.stephens.dailydozen.data

import app.cash.sqldelight.db.SqlDriver

/**
 * Creates a platform-specific [SqlDriver] for the Daily Dozen database.
 *
 * Each platform provides its own driver (Android: AndroidSqliteDriver, iOS:
 * NativeSqliteDriver, Wasm: WebWorkerDriver). [create] is `suspend` because the
 * web-worker driver loads its WASM/JS engine and applies the schema
 * asynchronously; the native drivers return immediately.
 *
 * The concrete actual classes intentionally differ in their constructors
 * (Android needs a `Context`), so this `expect` declares no constructor — each
 * platform's Koin module builds the factory.
 */
expect class DatabaseDriverFactory {
    suspend fun create(): SqlDriver
}
