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
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.ui.Logger

private const val KEY_ID_TOKEN = "id_token"


class AndroidTokenProvider(
    private val context: Context,
    private val appConfig: AppConfig,
    baseLogger: co.touchlab.kermit.Logger
) : TokenProvider {
    private val logger = baseLogger.withTag("AndroidTokenProvider")

    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "auth_tokens",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override suspend fun getToken(): String? =
        withContext(Dispatchers.IO) {
            val token = sharedPreferences.getString(KEY_ID_TOKEN, null)
            logger.d("AndroidTokenProvider: Retrieved token, exists: ${token != null}")
            token
        }

    override suspend fun refreshToken(): String? {
        return withContext(Dispatchers.IO) {
            try {
                logger.d("AndroidTokenProvider: Starting token refresh")

                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true) // Try to use existing account first
                    .setAutoSelectEnabled(true)
                    .setServerClientId(appConfig.googleClientId)
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
                            logger.d("AndroidTokenProvider: Token refresh successful")

                            newToken
                        } catch (e: GoogleIdTokenParsingException) {
                            logger.e("AndroidTokenProvider: Failed to parse refreshed token", e)
                            null
                        }
                    }

                    else -> {
                        logger.e("AndroidTokenProvider: Unexpected credential type during refresh: ${credential.type}")
                        null
                    }
                }

            } catch (e: GetCredentialException) {
                logger.e("AndroidTokenProvider: Token refresh failed", e)
                null
            } catch (e: Exception) {
                logger.e("AndroidTokenProvider: Unexpected error during token refresh", e)
                null
            }
        }
    }

    override suspend fun clearToken() {
        withContext(Dispatchers.IO) {
            logger.d("AndroidTokenProvider: Clearing token")
            sharedPreferences.edit { clear() }
        }
    }
}