package page.stephens.dailydozen.net

import android.content.Context

actual class SecureTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences("dailydozen_secure", Context.MODE_PRIVATE)

    actual fun getToken(): String? = prefs.getString(KEY, null)

    actual fun setToken(token: String?) {
        prefs.edit().apply {
            if (token == null) remove(KEY) else putString(KEY, token)
        }.apply()
    }

    private companion object {
        const val KEY = "authToken"
    }
}
