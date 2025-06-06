package org.jetbrains.demo

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.jetbrains.demo.di.androidModule
import org.jetbrains.demo.di.appModule
import org.jetbrains.demo.ui.App
import org.koin.android.ext.koin.androidContext
import org.koin.compose.KoinApplication
import org.koin.core.context.GlobalContext.startKoin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            KoinApplication(application = {
                androidContext(this@MainActivity)
                modules(appModule, androidModule)
            }) {
                App()
            }
        }
    }
}
