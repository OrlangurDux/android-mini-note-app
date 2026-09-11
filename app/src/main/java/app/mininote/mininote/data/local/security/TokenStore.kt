package app.mininote.mininote.data.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class StoredSession(
    val accessToken: String,
    val tokenType: String,
    /** Абсолютный момент истечения токена в секундах (см. project memory: `expires_in` от бэкенда
     * уже является абсолютным Unix-таймстампом, а не длительностью — прибавлять к нему now() не нужно). */
    val expiresAtEpochSeconds: Long,
)

@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "mininote_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun save(session: StoredSession) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_TOKEN_TYPE, session.tokenType)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds)
            .apply()
    }

    fun load(): StoredSession? {
        val token = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val tokenType = prefs.getString(KEY_TOKEN_TYPE, null) ?: "Bearer"
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0)
        return StoredSession(token, tokenType, expiresAt)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun isValid(nowEpochSeconds: Long = System.currentTimeMillis() / 1000): Boolean {
        val session = load() ?: return false
        return nowEpochSeconds < session.expiresAtEpochSeconds
    }

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
