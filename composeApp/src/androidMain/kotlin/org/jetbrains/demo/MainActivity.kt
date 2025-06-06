package org.jetbrains.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jetbrains.demo.auth.AuthViewModel
import org.jetbrains.demo.logging.Logger
import org.jetbrains.demo.auth.AndroidTokenProvider
import org.jetbrains.demo.network.HttpClient

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Logger.app.d("MainActivity: onCreate")
        Logger.auth.d("Configuring OAuth2 authentication")

        // Initialize TokenStorage
        val tokenProvider = AndroidTokenProvider(this)
        val authViewModel = AuthViewModel(tokenProvider)
        val client = HttpClient(tokenProvider)

        setContent {
            App(authViewModel = authViewModel, client = client)
        }
    }
}
