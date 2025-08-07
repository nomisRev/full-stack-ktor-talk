package org.jetbrains.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jetbrains.demo.di.androidModule
import org.jetbrains.demo.di.appModule
import org.jetbrains.demo.ui.hasToken
import org.jetbrains.demo.ui.App
import org.koin.compose.KoinMultiplatformApplication
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinConfiguration

class MainActivity : ComponentActivity() {

    @OptIn(KoinExperimentalAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            KoinMultiplatformApplication(
                koinConfiguration {
                    modules(appModule, androidModule)
                }) {
                App { hasToken() }
            }
        }
    }
}
