package page.stephens.dailydozen.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Android token storage: EncryptedSharedPreferences backed by an AES-256 key in
 * the Android Keystore. No plaintext fallback — if the encrypted store can't be
 * opened we let it throw rather than silently writing the JWT in the clear.
 */
actual class TokenStore(private val context: Context) {

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "dailydozen_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    actual suspend fun read(): String? = prefs.getString(KEY, null)

    actual suspend fun write(token: String) {
        prefs.edit().putString(KEY, token).apply()
    }

    actual suspend fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    private companion object {
        const val KEY = "jwt"
    }
}
