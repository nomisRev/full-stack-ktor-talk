package org.jetbrains.demo.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.demo.logging.Logger

class AuthViewModel(private val tokenStorage: TokenProvider) : ViewModel() {

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
        Logger.auth.d("AuthViewModel: Starting sign-in process")
        val newToken = tokenStorage.refreshToken()
        if (newToken != null) {
            Logger.auth.d("AuthViewModel: Token refresh successful")
            _isLoggedIn.value = true
        } else {
            Logger.auth.d("AuthViewModel: Token refresh failed")
            _isLoggedIn.value = false
        }
    }

    suspend fun signOut() {
        Logger.auth.d("AuthViewModel: Signing out user")
        tokenStorage.clearToken()
        _error.value = null
    }

    fun clearError() {
        Logger.auth.d("AuthViewModel: Clearing error state")
        _error.value = null
    }
}