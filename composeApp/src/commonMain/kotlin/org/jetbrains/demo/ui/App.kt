package org.jetbrains.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.chat.ChatScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    authViewModel: AuthViewModel = koinViewModel()
) {
    Logger.app.d("App: Composable started")

    val scope = rememberCoroutineScope()
    val state by authViewModel.state.collectAsState()

    MaterialTheme {
        if (state == AuthViewModel.AuthState.SignedIn) {
            ChatScreen(onSignOut = {
                scope.launch {
                    authViewModel.signOut()
                }
            })
        } else {
            Column(
                modifier = Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SignInContent(
                    state,
                    onClearError = { authViewModel.clearError() }
                ) { scope.launch { authViewModel.signIn() } }
            }
        }
    }
}

private enum class Screen {
    Chat, About
}
