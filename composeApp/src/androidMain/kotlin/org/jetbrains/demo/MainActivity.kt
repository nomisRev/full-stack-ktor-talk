package org.jetbrains.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.demo.auth.AuthViewModel
import org.jetbrains.demo.logging.Logger

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        Logger.app.d("MainActivity: onCreate")
        Logger.auth.d("Configuring OAuth2 authentication")
        
        setContent {
            val authViewModel: AuthViewModel = viewModel()
            App(authViewModel = authViewModel)
        }
    }
}
