package org.jetbrains.demo.network

import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.demo.logging.Logger

/**
 * Small utility function that confiures the Ktor HttpClient
 */
fun HttpClient(tokenProvider: TokenProvider): HttpClient = HttpClient {
    install(ContentNegotiation) { json() }

    install(Logging) {
        logger = object : io.ktor.client.plugins.logging.Logger {
            override fun log(message: String) = Logger.network.d(message)
        }
        level = LogLevel.INFO
    }

    install(Auth) {
        bearer {
            loadTokens {
                val token = tokenProvider.getToken()
                Logger.network.d("Loading token for request: ${token != null}")
                token?.let {
                    BearerTokens(accessToken = it, refreshToken = null)
                }
            }

            refreshTokens {
                Logger.network.d("Refreshing token...")
                val newToken = tokenProvider.refreshToken()
                if (newToken != null) {
                    Logger.network.d("Token refresh successful")
                    BearerTokens(accessToken = newToken, refreshToken = null)
                } else {
                    Logger.network.w("Token refresh failed")
                    null
                }
            }
        }
    }
}