package org.jetbrains.demo.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.demo.ui.Logger

class AuthViewModel(
    private val tokenStorage: TokenProvider,
    private val httpClient: HttpClient,
    base: co.touchlab.kermit.Logger
) : ViewModel() {
    private val logger = base.withTag("AuthViewModel")

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn

    init {
        viewModelScope.launch {
            _isLoggedIn.value = tokenStorage.getToken() != null
        }
    }

    suspend fun signIn() {
        logger.d("AuthViewModel: Starting sign-in process")
        val newToken = tokenStorage.refreshToken()
        if (newToken != null) {
            logger.d("AuthViewModel: Token refresh successful")
            registerUser()
            _isLoggedIn.value = true
        } else {
            logger.d("AuthViewModel: Token refresh failed")
            _isLoggedIn.value = false
        }
    }

    private suspend fun registerUser() {
        logger.d("AuthViewModel: Registering user")
        val response = httpClient.post("http://0.0.0.0:8080/user/register")
        if (response.status == HttpStatusCode.OK) {
            logger.d("AuthViewModel: User registration successful")
            _error.value = null
            _isLoggedIn.value = true
        } else {
            logger.d("AuthViewModel: User registration failed")
            _error.value = "Failed to register user"
            _isLoggedIn.value = false
            tokenStorage.clearToken()
        }
        _isLoading.value = false

    }

    suspend fun signOut() {
        tokenStorage.clearToken()
        _error.value = null
        _isLoggedIn.value = false
        _isLoading.value = false
    }

    fun clearError() {
        logger.d("AuthViewModel: Clearing error state")
        _error.value = null
    }
}