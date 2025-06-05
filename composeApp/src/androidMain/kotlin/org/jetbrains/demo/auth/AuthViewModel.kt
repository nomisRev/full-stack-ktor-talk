package org.jetbrains.demo.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.ktor.util.generateNonce
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.BuildConfig
import org.jetbrains.demo.logging.Logger

class AuthViewModel(private val tokenStorage: TokenStorage) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(tokenStorage.getIdToken() != null)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    fun signIn(context: Context) {
        Logger.auth.d("AuthViewModel: Starting sign-in process")

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(true)
                    .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
                    .setNonce(generateNonce())
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(context, request)

                handleSignInResult(result)

            } catch (e: GetCredentialException) {
                Logger.auth.e("AuthViewModel: GetCredentialException during sign-in", e)
                _error.value = "Sign-in failed: ${e.message}"
            } catch (e: Exception) {
                Logger.auth.e("AuthViewModel: Unexpected error during sign-in", e)
                _error.value = "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun handleSignInResult(result: GetCredentialResponse) {
        Logger.auth.d("AuthViewModel: Processing sign-in result")
        val credential = result.credential
        when {
            credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL -> {
                try {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    tokenStorage.saveIdToken(googleIdTokenCredential.idToken)

                    _isLoggedIn.value = true
                    Logger.auth.d("AuthViewModel: Sign-in successful for user: ${googleIdTokenCredential.displayName}")

                } catch (e: GoogleIdTokenParsingException) {
                    Logger.auth.e("AuthViewModel: Failed to parse Google ID token", e)
                    _error.value = "Failed to parse authentication token"
                }
            }

            else -> {
                Logger.auth.e("AuthViewModel: Unexpected credential type: ${credential.type}")
                _error.value = "Unexpected credential type: ${credential.type}"
            }
        }
    }

    fun signOut() {
        Logger.auth.d("AuthViewModel: Signing out user")
        tokenStorage.clearTokens()
        _error.value = null
    }

    fun clearError() {
        Logger.auth.d("AuthViewModel: Clearing error state")
        _error.value = null
    }
}