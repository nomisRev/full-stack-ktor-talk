package org.jetbrains.demo.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.chat.ChatScreen
import org.jetbrains.demo.journey.JourneyPlannerScreen
import org.koin.compose.koinInject

interface AuthSession {
    fun hasToken(): Boolean
    fun clearToken()
}

fun AuthSession(auth: AuthViewModel): AuthSession = object : AuthSession {
    override fun hasToken(): Boolean = auth.state.value is AuthViewModel.AuthState.SignedIn
    override fun clearToken() = auth.signOut()
}

@Composable
fun App(
    onNavHostReady: suspend (NavController) -> Unit = {},
    authSession: AuthSession,
) {
    Logger.app.d("App: Composable started")
    val navController = rememberNavController()
    val start = if (authSession.hasToken()) Screen.Chat else Screen.LogIn

    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Logger.app.d("App: Creating NavHost with $start")
            NavHost(navController, start) {
                Logger.app.d("NavHost building")
                composable<Screen.Chat> {
                    Logger.app.d("NavHost: Screen.Chat")
                    ChatScreen {
                        authSession.clearToken()
                        navController.navigate(Screen.LogIn)
                    }
                }
                composable<Screen.LogIn> {
                    Logger.app.d("NavHost: Screen.LogIn")
                    SignInContent { navController.navigate(Screen.Chat) }
                }
                composable<Screen.Planner> {
                    Logger.app.d("NavHost: Screen.Planner")
                    JourneyPlannerScreen()
                }
            }
        }
    }

    LaunchedEffect(navController) {
        Logger.app.d("App: onNavHostReady")
        onNavHostReady(navController)
        Logger.app.d("App: onNavHostReady - Done")
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

    @Serializable
    @SerialName("planner")
    data object Planner
}
