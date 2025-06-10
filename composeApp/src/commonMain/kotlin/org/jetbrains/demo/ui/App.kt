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
import org.jetbrains.demo.ui.Logger
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AppComponent : KoinComponent {
    val authViewModel: AuthViewModel by inject()
    val client: HttpClient by inject()
}

@Composable
fun App() {
    Logger.app.d("App: Composable started")
    val appComponent = remember { AppComponent() }
    val authViewModel = appComponent.authViewModel
    val client = appComponent.client

    val scope = rememberCoroutineScope()
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    MaterialTheme {
        if (isLoggedIn) {
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
                    isLoading = isLoading,
                    error = error,
                    onClearError = { authViewModel.clearError() }
                ) { scope.launch { authViewModel.signIn() } }
            }
        }
    }
}

@Composable
private fun SignInContent(
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onSignInClick: () -> Unit
) {
    Logger.app.d("SignInContent: Displaying sign-in UI, isLoading: $isLoading, hasError: ${error != null}")
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

            if (error != null) {
                Logger.app.d("SignInContent: Displaying error: $error")
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
                            text = "Error: $error",
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
