package org.jetbrains.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import org.jetbrains.demo.auth.*
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    authViewModel: AuthViewModel = koinViewModel(),
    client: HttpClient = koinInject()
) {
    Logger.app.d("App: Composable started")

    val scope = rememberCoroutineScope()
    val state by authViewModel.state.collectAsState()

    MaterialTheme {
        if (state == AuthViewModel.AuthState.SignedIn) {
            ChatScreen(client, onSignOut = {
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

@Composable
private fun SignInContent(
    state: AuthViewModel.AuthState,
    onClearError: () -> Unit,
    onSignInClick: () -> Unit
) {
    Logger.app.d("SignInContent: Displaying sign-in UI $state")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Google OAuth Demo",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sign in with your Google account to continue",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state is AuthViewModel.AuthState.Error) {
                val message = state.message.ifBlank { "Unknown error" }
                Logger.app.d("SignInContent: Displaying error: $message")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(onClick = {
                            Logger.app.d("SignInContent: Error dismiss button clicked")
                            onClearError()
                        }) {
                            Text("Dismiss")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            val isLoading = state is AuthViewModel.AuthState.Loading
            Button(
                onClick = {
                    // Trigger Google Sign-In flow
                    Logger.app.d("SignInContent: Sign in button clicked")
                    onSignInClick()
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    Logger.app.d("SignInContent: Showing loading indicator")
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Signing in..." else "Sign in with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This app uses Google Sign-In for authentication.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
