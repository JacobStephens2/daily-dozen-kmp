package page.stephens.dailydozen.data.auth

/**
 * Securely stores the bearer JWT, one implementation per platform (iOS Keychain
 * / Android EncryptedSharedPreferences / Wasm localStorage).
 *
 * THE LOAD-BEARING RULE (ADR-2): [clear] is the ONLY way a token is removed,
 * and it must be called solely from explicit user logout — NEVER from a 401
 * handler. A 401 triggers a token refresh, not a logout. "Auth loss is not data
 * loss." This single constraint is the direct inversion of the Chart35
 * silent-logout outage, and it is enforced by a CI guard (M6).
 *
 * Like [page.stephens.dailydozen.data.DatabaseDriverFactory], the actuals differ
 * in their constructors (Android needs a Context), so this expect declares none;
 * each platform's Koin module builds it.
 */
expect class TokenStore {
    suspend fun read(): String?
    suspend fun write(token: String)
    suspend fun clear()
}
