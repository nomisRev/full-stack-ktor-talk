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
import org.jetbrains.demo.ui.AuthSession
import org.koin.core.context.startKoin

object WebAuthSession : AuthSession {
    override fun hasToken(): Boolean {
        val currentUrl = window.location.href
        return when {
            currentUrl.contains("login") -> false
            else -> true
        }
    }

    override fun clearToken() {
        window.location.href = "/logout"
    }
}

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalBrowserHistoryApi::class
)
fun main() {
    startKoin { modules(appModule) }
    ComposeViewport("ComposeApp") {
        MaterialTheme {
            App({ controller ->
                window.bindToNavigation(controller) { entry ->
                    val route = entry.destination.route.orEmpty()
                    when {
                        route == "chat" -> ""
                        else -> route
                    }
                }
            }, WebAuthSession)
        }
    }
}
