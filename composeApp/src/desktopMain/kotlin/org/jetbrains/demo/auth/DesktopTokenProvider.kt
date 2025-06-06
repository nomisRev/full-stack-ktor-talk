package org.jetbrains.demo.auth

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
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
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import org.jetbrains.demo.config.*
import org.jetbrains.demo.logging.*
import org.jetbrains.demo.network.*
import java.awt.*
import java.net.*
import java.net.ServerSocket

/**
 * Desktop implementation of TokenProvider using Google OAuth2 flow with local HTTP server.
 * This implementation:
 * 1. Starts a local CIO server to handle OAuth callbacks
 * 2. Opens browser to Google OAuth URL
 * 3. Exchanges authorization code for ID token using Ktor HTTP client
 */
class DesktopTokenProvider(private val appConfig: AppConfig) : TokenProvider {

    private val httpClient = HttpClient(io.ktor.client.engine.cio.CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        followRedirects = false
    }

    @Serializable
    data class TokenResponse(
        @SerialName("access_token") val accessToken: String? = null,
        @SerialName("id_token") val idToken: String? = null,
        @SerialName("token_type") val tokenType: String? = null,
        @SerialName("expires_in") val expiresIn: Int? = null,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("scope") val scope: String? = null
    )

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        Logger.app.d("DesktopTokenProvider: Token Storage not yet implemented.")
        null
    }

    override suspend fun refreshToken(): String? = withContext(Dispatchers.IO) {
        Logger.network.d("DesktopTokenProvider: Starting OAuth2 flow")

        val callback = CompletableDeferred<OAuth2>()
        val availablePort = ServerSocket(0).use { it.localPort }
        val server = embeddedServer(CIO, port = availablePort) {
            authentication {
                oauth("oauth") {
                    urlProvider = { "http://localhost:$availablePort/callback" }
                    providerLookup = {
                        OAuthServerSettings.OAuth2ServerSettings(
                            name = "google",
                            authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                            accessTokenUrl = "https://oauth2.googleapis.com/token",
                            requestMethod = HttpMethod.Post,
                            clientId = DesktopConfig.googleClientId,
                            clientSecret = DesktopConfig.clientSecret,
                            defaultScopes = listOf("email"),
                        )
                    }
                    client = HttpClient(io.ktor.client.engine.cio.CIO) { install(ContentNegotiation) { json() } }
                }
            }
            routing {
                authenticate("oauth") {
                    get("/login") {}
                    get("/callback") {
                        val principal: OAuth2? = call.authentication.principal()
                        if (principal == null) {
                            callback.completeExceptionally(IllegalStateException("No OAuth2 principal"))
                            call.respondText(createErrorResponseHtml("No OAuth2 principal"), ContentType.Text.Html)
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
            val response = httpClient.get("http://localhost:$availablePort/login")
            if (response.status == HttpStatusCode.Found) {
                val url = response.headers["Location"]!!
                Desktop.getDesktop().browse(URI(url))
            } else {
                throw IllegalStateException("")
            }
            callback.await().extraParameters["id_token"]
        } finally {
            server.stopSuspend(1000, 5000)
        }
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        Logger.network.d("DesktopTokenProvider: Clearing token")
        // TODO implement
    }

    private fun createSuccessResponseHtml(): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Authentication Successful</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                .success { color: green; }
            </style>
        </head>
        <body>
            <h1 class="success">Authentication Successful!</h1>
            <p>You can now close this browser window and return to the application.</p>
        </body>
        </html>
    """.trimIndent()

    private fun createErrorResponseHtml(error: String): String = """
        <!DOCTYPE html>
        <html>
        <head>
            <title>Authentication Error</title>
            <style>
                body { font-family: Arial, sans-serif; text-align: center; margin-top: 50px; }
                .error { color: red; }
            </style>
        </head>
        <body>
            <h1 class="error">Authentication Error</h1>
            <p>Error: $error</p>
            <p>Please close this browser window and try again.</p>
        </body>
        </html>
    """.trimIndent()
}