package org.jetbrains.demo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.chat.ChatScreen
import org.koin.compose.koinInject

@Composable
fun hasToken(): Boolean {
    val authViewModel = koinInject<AuthViewModel>()
    return authViewModel.state.collectAsStateWithLifecycle().value is AuthViewModel.AuthState.SignedIn
}

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {},
    isLoggedIn: @Composable () -> Boolean,
) {
    Logger.app.d("App: Composable started")
    val navController = rememberNavController()
    val start = if (isLoggedIn()) Screen.Chat else Screen.LogIn

    MaterialTheme {
        NavHost(navController, start) {

            composable<Screen.Chat> {
                ChatScreen { navController.navigate(Screen) }
            }
            composable<Screen.LogIn> {
                SignInContent { navController.navigate(Screen.Chat) }
            }
        }
    }

    LaunchedEffect(navController) {
        onNavHostReady(navController)
    }
}

@Serializable
object Screen {
    @Serializable
    @SerialName("chat")
    data object Chat

    @Serializable
    @SerialName("login")
    data object LogIn
}
