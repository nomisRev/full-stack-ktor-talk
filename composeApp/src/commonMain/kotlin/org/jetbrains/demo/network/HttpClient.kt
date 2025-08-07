package org.jetbrains.demo.network

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.*
import org.jetbrains.demo.auth.TokenProvider

/**
 * [TokenProvider] is optional since browser works with sessions.
 */
fun HttpClient(
    tokenProvider: TokenProvider?,
    baseLogger: Logger,
): HttpClient = HttpClient {
    val logger = baseLogger.withTag("HttpClient")
    install(ContentNegotiation) { json() }
    install(SSE)
    if (tokenProvider != null) withAuthBearer(tokenProvider, logger)
}

private fun HttpClientConfig<*>.withAuthBearer(tokenProvider: TokenProvider, logger: Logger) {
    install(Auth) {
        bearer {
            loadTokens {
                val token = tokenProvider.getToken() ?: tokenProvider.refreshToken()
                logger.d("Loading token for request: ${token != null}")
                token?.let {
                    BearerTokens(accessToken = it, refreshToken = null)
                }
            }

            refreshTokens {
                logger.d("Refreshing token...")
                val newToken = tokenProvider.refreshToken()
                if (newToken != null) {
                    logger.d("Token refresh successful")
                    BearerTokens(accessToken = newToken, refreshToken = null)
                } else {
                    logger.w("Token refresh failed")
                    null
                }
            }
        }
    }
}
