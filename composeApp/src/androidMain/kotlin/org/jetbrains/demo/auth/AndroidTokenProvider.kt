package org.jetbrains.demo.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.ktor.util.generateNonce
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.BuildConfig
import org.jetbrains.demo.logging.Logger
import org.jetbrains.demo.network.TokenProvider

private const val KEY_ID_TOKEN = "id_token"

class AndroidTokenProvider(private val context: Context) : TokenProvider {

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

    override suspend fun getToken(): String? =
        withContext(Dispatchers.IO) {
            val token = sharedPreferences.getString(KEY_ID_TOKEN, null)
            Logger.network.d("AndroidTokenProvider: Retrieved token, exists: ${token != null}")
            token
        }

    override suspend fun refreshToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                Logger.network.d("AndroidTokenProvider: Starting token refresh")

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true) // Try to use existing account first
                    .setAutoSelectEnabled(true)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setNonce(generateNonce())
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)

                val credential = result.credential
                when {
                    credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                        try {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            val newToken = googleIdTokenCredential.idToken

                            sharedPreferences.edit { putString(KEY_ID_TOKEN, newToken) }
                            Logger.network.d("AndroidTokenProvider: Token refresh successful")

                            newToken
                        } catch (e: GoogleIdTokenParsingException) {
                            Logger.network.e("AndroidTokenProvider: Failed to parse refreshed token", e)
                            null
                        }
                    }

                    else -> {
                        Logger.network.e("AndroidTokenProvider: Unexpected credential type during refresh: ${credential.type}")
                        null
                    }
                }

            } catch (e: GetCredentialException) {
                Logger.network.e("AndroidTokenProvider: Token refresh failed", e)
                null
            } catch (e: Exception) {
                Logger.network.e("AndroidTokenProvider: Unexpected error during token refresh", e)
                null
            }
        }
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            Logger.network.d("AndroidTokenProvider: Clearing token")
            sharedPreferences.edit { clear() }
        }
    }
}