package page.stephens.bountywell.data.auth

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
        // Confirmed to match TOKEN_KEY in the web app's js/auth.js. Both are
        // served from bountywell.com and share this localStorage entry, so the
        // legacy name stays deliberately un-renamed: changing it here would sign
        // existing users out and desync the two apps.
        const val KEY = "dailyDozenAuthToken"
    }
}
