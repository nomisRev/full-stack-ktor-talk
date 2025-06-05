package org.jetbrains.demo.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.jetbrains.demo.logging.Logger
import androidx.core.content.edit

private const val KEY_ID_TOKEN = "id_token"

class TokenStorage(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveIdToken(idToken: String) {
        Logger.auth.d("TokenStorage: Saving ID token securely")
        sharedPreferences.edit {
            putString(KEY_ID_TOKEN, idToken)
        }
    }

    fun getIdToken(): String? {
        val token = sharedPreferences.getString(KEY_ID_TOKEN, null)
        Logger.auth.d("TokenStorage: Retrieved ID token, exists: ${token != null}")
        return token
    }

    fun clearTokens() {
        Logger.auth.d("TokenStorage: Clearing all tokens")
        sharedPreferences.edit {
            clear()
        }
    }
}