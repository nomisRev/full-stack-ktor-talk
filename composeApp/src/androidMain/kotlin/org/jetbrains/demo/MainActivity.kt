package org.jetbrains.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jetbrains.demo.auth.AuthViewModel
import org.jetbrains.demo.auth.AndroidTokenProvider
import org.jetbrains.demo.network.HttpClient
import org.jetbrains.demo.ui.App

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val tokenProvider = AndroidTokenProvider(this)
        val authViewModel = AuthViewModel(tokenProvider)
        val client = HttpClient(tokenProvider)

        setContent {
            App(authViewModel = authViewModel, client = client)
        }
    }
}
