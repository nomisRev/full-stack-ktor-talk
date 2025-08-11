package org.jetbrains.demo.auth

import co.touchlab.kermit.Logger
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.engine.cio.CIO as CIOClient
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.auth.OAuthAccessTokenResponse.OAuth2
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.oauth
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.*
import kotlinx.html.*
import kotlinx.html.stream.createHTML
import kotlinx.serialization.json.*
import org.jetbrains.demo.config.*
import java.awt.*
import java.net.*

private const val KEY_ID_TOKEN = "id_token"

/**
 * Desktop implementation of TokenProvider using Google OAuth2 flow with local HTTP server.
 * This implementation:
 * 1. Starts a local CIO server to handle OAuth callbacks
 * 2. Opens browser to Google OAuth URL
 * 3. Exchanges authorization code for ID token using Ktor HTTP client
 */
class DesktopTokenProvider(
    private val config: DesktopConfig,
    private val preferences: EncryptedPreferences,
    base: Logger,
) : TokenProvider {
    private val logger = base.withTag("DesktopTokenProvider")

    override fun getToken(): String? {
        val token = preferences.get(KEY_ID_TOKEN, null)
        logger.d("TokenProvider: Retrieved token, exists: ${token != null}")
        return token
    }

    override fun clearToken() {
        logger.d("Clearing token")
        preferences.remove(KEY_ID_TOKEN)
    }

    override suspend fun refreshToken(): String? = withContext(Dispatchers.IO) {
        logger.d("Refreshing token")
        HttpClient(CIOClient) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }.use { httpClient ->
            val callback = CompletableDeferred<OAuth2>()
            val server = embeddedServer(CIO) {
                val port = async { engine.resolvedConnectors().first().port }
                authentication {
                    oauth("oauth") {
                        @OptIn(ExperimentalCoroutinesApi::class)
                        urlProvider = { "http://localhost:${port.getCompleted()}/callback" }
                        providerLookup = {
                            OAuthServerSettings.OAuth2ServerSettings(
                                name = "google",
                                authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                                accessTokenUrl = "https://oauth2.googleapis.com/token",
                                requestMethod = HttpMethod.Post,
                                clientId = config.clientId,
                                clientSecret = config.clientSecret,
                                defaultScopes = listOf("email"),
                            )
                        }
                        client = httpClient
                    }
                }
                routing {
                    authenticate("oauth") {
                        get("/login") {}
                        get("/callback") {
                            val principal: OAuth2? = call.authentication.principal()
                            if (principal == null) {
                                callback.completeExceptionally(IllegalStateException("No OAuth2 principal"))
                                call.respondText(createErrorResponseHtml(), ContentType.Text.Html)
                            } else {
                                callback.complete(principal)
                                call.respondText(createSuccessResponseHtml(), ContentType.Text.Html)
                            }
                        }
                    }
                }
            }

            try {
                server.startSuspend(wait = false)
                logger.d("Refreshing token. Server started.")
                val port = server.engine.resolvedConnectors().first().port
                val response = httpClient.config {
                    followRedirects = false
                }.get("http://localhost:$port/login")
                val url = requireNotNull(response.headers["Location"]) {
                    "Expected Location header and 302 Found, but found ${response.status}."
                }
                logger.d("Refreshing token. Opening browser.")
                Desktop.getDesktop().browse(URI(url))
                val oauth = callback.await()
                val idToken = oauth.extraParameters[KEY_ID_TOKEN]
                if (idToken != null) preferences.put(KEY_ID_TOKEN, idToken)
                logger.d("Received, and stored token.")
                oauth.extraParameters[KEY_ID_TOKEN]
            } finally {
                withContext(NonCancellable) {
                    server.stopSuspend(1000, 5000)
                    httpClient.close()
                }
            }
        }
    }

    private fun createSuccessResponseHtml(): String = createHTML().html {
        head {
            title("Authentication Successful")
            style {
                unsafe {
                    +createSuccessPageStyles()
                }
            }
        }
        body {
            div("container") {
                div("success-icon") { +"✅" }
                h1 { +"Authentication Successful!" }
                p { +"Great! You have been successfully authenticated." }
                div("close-instruction") {
                    +"You can now close this browser window and return to the application."
                }
            }
        }
    }

    private fun createErrorResponseHtml(): String = createHTML().html {
        head {
            title("Authentication Error")
            style {
                unsafe {
                    +createErrorPageStyles()
                }
            }
        }
        body {
            div("container") {
                div("error-icon") { +"❌" }
                h1 { +"Authentication Error" }
                p { +"Something went wrong during the authentication process." }
                div("retry-instruction") {
                    +"Please close this browser window and try again."
                }
            }
        }
    }

    private fun createSuccessPageStyles(): String =
        """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                background: linear-gradient(135deg, #4f46e5 0%, #7c3aed 50%, #ec4899 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                position: relative;
            }
            
            body::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: 
                    radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.3) 0%, transparent 50%),
                    radial-gradient(circle at 80% 20%, rgba(255, 119, 198, 0.3) 0%, transparent 50%),
                    radial-gradient(circle at 40% 40%, rgba(120, 219, 255, 0.2) 0%, transparent 50%);
            }
            
            .container {
                background: rgba(255, 255, 255, 0.15);
                backdrop-filter: blur(20px);
                border-radius: 24px;
                padding: 3rem 2.5rem;
                text-align: center;
                box-shadow: 
                    0 25px 50px rgba(0, 0, 0, 0.15),
                    0 0 0 1px rgba(255, 255, 255, 0.1),
                    inset 0 1px 0 rgba(255, 255, 255, 0.2);
                max-width: 520px;
                width: 90%;
                position: relative;
                z-index: 1;
            }
            
            .success-icon {
                font-size: 5rem;
                margin-bottom: 1.5rem;
                display: inline-block;
                filter: drop-shadow(0 0 20px rgba(34, 197, 94, 0.5));
            }
            
            h1 {
                font-size: 2.75rem;
                margin-bottom: 1.5rem;
                font-weight: 700;
                background: linear-gradient(135deg, #ffffff 0%, #f0f9ff 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
                line-height: 1.2;
            }
            
            p {
                font-size: 1.3rem;
                line-height: 1.7;
                opacity: 0.95;
                margin-bottom: 2.5rem;
                color: rgba(255, 255, 255, 0.9);
            }
            
            .close-instruction {
                background: rgba(255, 255, 255, 0.1);
                border-radius: 16px;
                padding: 1.5rem;
                font-size: 1.1rem;
                border: 1px solid rgba(255, 255, 255, 0.2);
                color: rgba(255, 255, 255, 0.9);
                transition: background-color 0.3s ease;
            }
            
            .close-instruction:hover {
                background: rgba(255, 255, 255, 0.15);
            }
        """.trimIndent()

    private fun createErrorPageStyles(): String =
        """
            * {
                margin: 0;
                padding: 0;
                box-sizing: border-box;
            }
            
            body {
                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
                background: linear-gradient(135deg, #dc2626 0%, #ea580c 50%, #d97706 100%);
                min-height: 100vh;
                display: flex;
                align-items: center;
                justify-content: center;
                color: white;
                position: relative;
            }
            
            body::before {
                content: '';
                position: absolute;
                top: 0;
                left: 0;
                right: 0;
                bottom: 0;
                background: 
                    radial-gradient(circle at 20% 80%, rgba(220, 38, 38, 0.3) 0%, transparent 50%),
                    radial-gradient(circle at 80% 20%, rgba(234, 88, 12, 0.3) 0%, transparent 50%),
                    radial-gradient(circle at 40% 40%, rgba(217, 119, 6, 0.2) 0%, transparent 50%);
            }
            
            .container {
                background: rgba(255, 255, 255, 0.15);
                backdrop-filter: blur(20px);
                border-radius: 24px;
                padding: 3rem 2.5rem;
                text-align: center;
                box-shadow: 
                    0 25px 50px rgba(0, 0, 0, 0.15),
                    0 0 0 1px rgba(255, 255, 255, 0.1),
                    inset 0 1px 0 rgba(255, 255, 255, 0.2);
                max-width: 520px;
                width: 90%;
                position: relative;
                z-index: 1;
            }
            
            .error-icon {
                font-size: 5rem;
                margin-bottom: 1.5rem;
                display: inline-block;
                filter: drop-shadow(0 0 20px rgba(239, 68, 68, 0.5));
            }
            
            h1 {
                font-size: 2.75rem;
                margin-bottom: 1.5rem;
                font-weight: 700;
                background: linear-gradient(135deg, #ffffff 0%, #fef2f2 100%);
                -webkit-background-clip: text;
                -webkit-text-fill-color: transparent;
                background-clip: text;
                text-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
                line-height: 1.2;
            }
            
            .error-message {
                background: rgba(255, 255, 255, 0.1);
                border-radius: 16px;
                padding: 1.5rem;
                margin: 2rem 0;
                font-size: 1.1rem;
                border: 1px solid rgba(255, 255, 255, 0.2);
                word-break: break-word;
                color: rgba(255, 255, 255, 0.95);
                font-family: 'SF Mono', Monaco, 'Cascadia Code', 'Roboto Mono', Consolas, 'Courier New', monospace;
                text-align: left;
                box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
            }
            
            p {
                font-size: 1.3rem;
                line-height: 1.7;
                opacity: 0.95;
                margin-bottom: 2rem;
                color: rgba(255, 255, 255, 0.9);
            }
            
            .retry-instruction {
                background: rgba(255, 255, 255, 0.1);
                border-radius: 16px;
                padding: 1.5rem;
                font-size: 1.1rem;
                border: 1px solid rgba(255, 255, 255, 0.2);
                color: rgba(255, 255, 255, 0.9);
                transition: background-color 0.3s ease;
            }
            
            .retry-instruction:hover {
                background: rgba(255, 255, 255, 0.15);
            }
        """.trimIndent()
}