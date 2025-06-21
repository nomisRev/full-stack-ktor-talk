package org.jetbrains.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.defaultRequest
import org.jetbrains.BuildConfig
import org.jetbrains.demo.di.androidModule
import org.jetbrains.demo.di.appModule
import org.jetbrains.demo.network.HttpClient
import org.jetbrains.demo.ui.App
import org.koin.compose.KoinMultiplatformApplication
import org.koin.dsl.koinConfiguration

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        HttpClient(Android) {
            defaultRequest {
                url(BuildConfig.API_BASE_URL)
            }
        }

        setContent {
            KoinMultiplatformApplication(
                koinConfiguration {
                    modules(appModule, androidModule)
                }) {
                App()
            }
        }
    }
}
