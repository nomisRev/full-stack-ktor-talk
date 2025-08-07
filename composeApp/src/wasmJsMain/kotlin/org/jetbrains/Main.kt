package org.jetbrains

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToNavigation
import kotlinx.browser.document
import kotlinx.browser.window
import org.jetbrains.demo.di.appModule
import org.jetbrains.demo.ui.App
import org.koin.core.context.startKoin

fun isLoggedIn(): Boolean {
    val currentUrl = window.location.href
    // If you're not authenticated you're always redirected to the login page.
    return when {
        currentUrl.contains("login") -> false
        else -> true
    }
}

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {
    val body = document.body ?: return
    startKoin { modules(appModule) }
    ComposeViewport(body) {
        MaterialTheme {
            App({ controller -> window.bindToNavigation(controller) }) { isLoggedIn() }
        }
    }
}
