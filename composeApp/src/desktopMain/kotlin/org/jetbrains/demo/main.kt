package org.jetbrains.demo

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.jetbrains.demo.di.appModule
import org.jetbrains.demo.di.desktopModule
import org.jetbrains.demo.ui.App
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(appModule, desktopModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "Demo App",
    ) {
        App()
    }
}