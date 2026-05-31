package page.stephens.dailydozen.net

/**
 * Persists the auth JWT in the platform's secure store (iOS Keychain, Android
 * private prefs, web localStorage). The token is deliberately kept out of the
 * synced JSON blob.
 */
expect class SecureTokenStore {
    fun getToken(): String?
    fun setToken(token: String?)
}
