package io.ktor.server.auth.openid

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.config.*
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.cio.CIO as SCIO
import io.ktor.server.cio.CIOApplicationEngine
import io.ktor.server.engine.ApplicationEnvironmentBuilder
import io.ktor.server.engine.applicationEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.OAuth2Config
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.Assert.fail
import java.net.BindException
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

/**
 * Base test class that provides MockOAuth2Server setup for OpenID Connect tests
 */
abstract class OpenIdConnectTestBase {

    protected lateinit var engine: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>
    protected val mockOAuth2Server: MockOAuth2Server by lazy {
        val config = OAuth2Config()
        MockOAuth2Server(config).also { mockOAuth2Server ->
            // Try to start the server with retries in case of port conflicts
            var retries = 0
            var started = false

            while (!started && retries < maxRetries) {
                try {
                    mockOAuth2Server.start(mockServerPort + retries)
                    started = true
                } catch (e: BindException) {
                    retries++
                    println("Port ${mockServerPort + retries - 1} is already in use, retrying with port ${mockServerPort + retries}")
                } catch (e: Exception) {
                    fail("Failed to start mock OAuth2 server: ${e.message}")
                }
            }

            if (!started) {
                fail("Failed to start mock OAuth2 server after $maxRetries retries")
            }
        }
    }
    protected val testIssuer = "default"
    protected val testClientId = "test-client"
    protected val testClientSecret = "test-secret"

    // Default scopes for testing
    protected val defaultScopes = listOf("openid", "profile", "email")

    // Default port for the mock server
    private val mockServerPort = 8081

    // Maximum number of retries for starting the server
    private val maxRetries = 3
    private val serverPort = runBlocking { engine.application.engine.resolvedConnectors().single().port }
    val client by lazy {
        HttpClient(CIO) {
            defaultRequest { port = serverPort }
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    @BeforeTest
    fun setUp() {
        engine = embeddedServer(
            SCIO,
            environment = applicationEnvironment { environment() },
            configure = {
                connector {
                    host = "0.0.0.0"
                    port = 0
                }
            }
        ) {
            module(createTestClient())
        }.start(wait = false)

//        engine.application.engine.resolvedConnectors().single()
    }

    @AfterTest
    fun tearDown() {
        mockOAuth2Server.shutdown()
        engine.stop(1, 1, TimeUnit.SECONDS)
    }

    /**
     * Get the base URL of the mock OAuth2 server
     */
    protected fun getMockServerUrl(): String = mockOAuth2Server.baseUrl().toString().trimEnd('/')

    /**
     * Get the issuer URL for the test issuer
     */
    protected fun getIssuerUrl(): String = "${getMockServerUrl()}/$testIssuer"

    /**
     * Get the well-known configuration URL
     */
    protected fun getWellKnownUrl(): String = "${getIssuerUrl()}/.well-known/openid-configuration"

    /**
     * Get the JWKS URL
     */
    protected fun getJwksUrl(): String = "${getIssuerUrl()}/jwks"

    /**
     * Get the token endpoint URL
     */
    protected fun getTokenEndpointUrl(): String = "${getIssuerUrl()}/token"

    /**
     * Create a test token with default claims
     *
     * @param subject The subject of the token
     * @param claims Additional claims to include in the token
     * @param expiry The expiry time in seconds
     * @param scopes The scopes to include in the token
     * @return The serialized token
     */
    protected fun createTestToken(
        subject: String = "test-user",
        claims: Map<String, Any> = mapOf(
            "name" to "Test User",
            "email" to "test@example.com",
            "preferred_username" to "testuser"
        ),
        expiry: Long = 3600,
        scopes: List<String> = defaultScopes
    ): String {
        return mockOAuth2Server.issueToken(
            issuerId = testIssuer,
            clientId = testClientId,
            tokenCallback = DefaultOAuth2TokenCallback(
                issuerId = testIssuer,
                subject = subject,
                audience = listOf(testClientId),
                claims = claims,
                expiry = expiry,
            )
        ).serialize()
    }

    /**
     * Create a token with refresh token
     *
     * @param subject The subject of the token
     * @param claims Additional claims to include in the token
     * @param expiry The expiry time in seconds
     * @param scopes The scopes to include in the token
     * @return A pair of the ID token and refresh token
     */
    protected fun createTokenWithRefreshToken(
        subject: String = "test-user",
        claims: Map<String, Any> = mapOf(
            "name" to "Test User",
            "email" to "test@example.com",
            "preferred_username" to "testuser"
        ),
        expiry: Long = 3600,
        scopes: List<String> = defaultScopes
    ): Pair<String, String> {
        val token = mockOAuth2Server.issueToken(
            issuerId = testIssuer,
            clientId = testClientId,
            tokenCallback = DefaultOAuth2TokenCallback(
                issuerId = testIssuer,
                subject = subject,
                audience = listOf(testClientId),
                claims = claims,
                expiry = expiry,
            )
        )

        // The mock-oauth2-server doesn't provide a real refresh token, so we'll create one
        val refreshToken = mockOAuth2Server.issueToken(
            issuerId = testIssuer,
            clientId = testClientId,
            tokenCallback = DefaultOAuth2TokenCallback(
                issuerId = testIssuer,
                subject = subject,
                audience = listOf(testClientId),
                claims = mapOf("token_type" to "refresh_token") + claims,
                expiry = 86400, // 24 hours
            )
        ).serialize()

        return Pair(token.serialize(), refreshToken)
    }

    /**
     * Create an expired test token for testing token validation
     *
     * @param subject The subject of the token
     * @param claims Additional claims to include in the token
     * @return The serialized token
     */
    protected fun createExpiredTestToken(
        subject: String = "test-user",
        claims: Map<String, Any> = mapOf(
            "name" to "Test User",
            "email" to "test@example.com"
        )
    ): String {
        return createTestToken(subject, claims, expiry = -1) // Already expired
    }

    /**
     * Create a malformed token for testing error handling
     */
    protected fun createMalformedToken(): String {
        return "invalid.token.format"
    }

    /**
     * Create a configuration for the OpenID Connect provider
     */
    fun config(): OpenIdConfig = OpenIdConfig(
        getIssuerUrl(),
        testClientId,
        testClientSecret,
        defaultScopes
    )

    /**
     * Configure test application with mock OAuth2 server
     */
    protected fun ApplicationEnvironmentBuilder.environment() {
        config = MapApplicationConfig().apply {
            put("ktor.openid.openid-connect.issuer", getIssuerUrl())
            put("ktor.openid.openid-connect.clientId", testClientId)
            put("ktor.openid.openid-connect.clientSecret", testClientSecret)
            put("ktor.openid.openid-connect.scopes", defaultScopes)
        }
    }

    /**
     * Create authorization header with Bearer token
     */
    protected fun createAuthorizationHeader(token: String): String {
        return "Bearer $token"
    }

    /**
     * Create a token with custom audience for testing audience validation
     *
     * @param audience The audience to include in the token
     * @param subject The subject of the token
     * @return The serialized token
     */
    protected fun createTokenWithAudience(
        audience: List<String>,
        subject: String = "test-user"
    ): String {
        return mockOAuth2Server.issueToken(
            issuerId = testIssuer,
            clientId = testClientId,
            tokenCallback = DefaultOAuth2TokenCallback(
                issuerId = testIssuer,
                subject = subject,
                audience = audience,
                claims = mapOf("name" to "Test User"),
                expiry = 3600
            )
        ).serialize()
    }

    /**
     * Create a token with custom issuer for testing issuer validation
     *
     * @param issuer The issuer to include in the token
     * @param subject The subject of the token
     * @return The serialized token
     */
    protected fun createTokenWithIssuer(
        issuer: String,
        subject: String = "test-user"
    ): String {
        return mockOAuth2Server.issueToken(
            issuerId = issuer,
            clientId = testClientId,
            tokenCallback = DefaultOAuth2TokenCallback(
                issuerId = issuer,
                subject = subject,
                audience = listOf(testClientId),
                claims = mapOf("name" to "Test User"),
                expiry = 3600
            )
        ).serialize()
    }

    /**
     * Create a test HttpClient for making requests to the mock server
     */
    protected fun createTestClient(): HttpClient {
        return HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    /**
     * Create a token with missing claims for testing error handling
     *
     * @param missingClaims List of standard claims to exclude from the token
     * @param subject The subject of the token
     * @return The serialized token
     */
    protected fun createTokenWithMissingClaims(
        missingClaims: List<String>,
        subject: String = "test-user"
    ): String {
        val defaultClaims = mapOf(
            "name" to "Test User",
            "email" to "test@example.com",
            "preferred_username" to "testuser"
        )

        val filteredClaims = defaultClaims.filterKeys { key -> !missingClaims.contains(key) }

        return createTestToken(subject, filteredClaims)
    }
}