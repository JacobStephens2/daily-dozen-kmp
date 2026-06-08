package page.stephens.dailydozen.data.auth

import kotlinx.browser.localStorage

/**
 * Wasm token storage: browser localStorage, matching the web client (§2). Note
 * localStorage is per-origin, so this shares a session with the web PWA only
 * when served from the same origin. The key mirrors the web client's.
 */
actual class TokenStore {
    actual suspend fun read(): String? = localStorage.getItem(KEY)

    actual suspend fun write(token: String) {
        localStorage.setItem(KEY, token)
    }

    actual suspend fun clear() {
        localStorage.removeItem(KEY)
    }

    private companion object {
        // TODO(web-parity): confirm this matches js/auth.js's token key before
        // deploying the Wasm build to the same origin as the web PWA.
        const val KEY = "dailyDozenAuthToken"
    }
}
