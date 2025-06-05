package org.jetbrains.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.logging.Logger
@Composable
fun App(authViewModel: AuthViewModel) {
    Logger.app.d("App: Composable started")
    
    val context = LocalContext.current
    val isLoading by authViewModel.isLoading.collectAsState()
    val error by authViewModel.error.collectAsState()
    val userInfo by authViewModel.userInfo.collectAsState()

    MaterialTheme {
        Column(
            modifier = Modifier
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (userInfo != null) {
                SignedInContent(
                    user = userInfo!!,
                    onSignOut = { authViewModel.signOut() }
                )
            } else {
                SignInContent(
                    isLoading = isLoading,
                    error = error,
                    onSignIn = { /* Not used */ },
                    onClearError = { authViewModel.clearError() },
                    onSignInClick = { authViewModel.signIn(context) }
                )
            }
        }
    }
}
@Composable
private fun SignedInContent(
    user: UserInfo,
    onSignOut: () -> Unit
) {
    Logger.app.d("SignedInContent: Displaying content for user ${user.email}")
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
                text = "Welcome!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Name: ${user.name}",
                style = MaterialTheme.typography.bodyLarge
            )

            Text(
                text = "Email: ${user.email}",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Sign Out")
            }
        }
    }
}
@Composable
private fun SignInContent(
    isLoading: Boolean,
    error: String?,
    onSignIn: (String) -> Unit,
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
