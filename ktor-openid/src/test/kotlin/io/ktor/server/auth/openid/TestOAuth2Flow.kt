package io.ktor.server.auth.openid

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.setCookie
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the OAuth2 authentication flow
 */
class TestOAuth2Flow : OpenIdConnectTestBase() {

    /**
     * Test the login redirect
     *
     * This test verifies that the login endpoint redirects to the authorization endpoint
     */
    @Test
    fun `test login redirect`() = runBlocking {
        // Make a request to the login endpoint
        val response = client.config {
            followRedirects = false
        }.get("/oauth/openid-connect-oauth/login")

        // Verify that the response is a redirect
        assertEquals(HttpStatusCode.Found, response.status)

        // Get the location header
        val location = response.headers["Location"]
        assertNotNull(location)

        // Verify that the location is the authorization endpoint
        assertTrue(location.startsWith(getIssuerUrl()), "Location should start with issuer URL")
        assertTrue(location.contains("/authorize"), "Location should contain /authorize")

        // Verify that the required OAuth2 parameters are included
        assertTrue(location.contains("client_id=$testClientId"), "Location should contain client_id")
        assertTrue(location.contains("redirect_uri="), "Location should contain redirect_uri")
        assertTrue(location.contains("response_type=code"), "Location should contain response_type=code")
        assertTrue(location.contains("scope="), "Location should contain scope")
    }

    private val oAuthClient = HttpClient(CIO) {
        install(ContentNegotiation) { json() }
    }

    /**
     * Test the callback endpoint
     *
     * This test simulates the callback from the authorization server with an authorization code
     */
    @Test
    fun `test callback with authorization code`() = runBlocking {
        // Create a test token that will be returned by the mock server
        val testSubject = "test-subject-456"
        val testName = "Jane Smith"
        val testEmail = "jane.smith@example.com"

        val claims = mapOf(
            "name" to testName,
            "email" to testEmail
        )

        // Issue a token that the mock server will return when the code is exchanged
        mockOAuth2Server.enqueueCallback(
            DefaultOAuth2TokenCallback(
                issuerId = testIssuer,
                subject = testSubject,
                audience = listOf(testClientId),
                claims = claims,
                expiry = 3600,
            )
        )

        // Simulate the callback from the authorization server
        val response = client.get("/oauth/openid-connect-oauth/redirect?code=test-authorization-code&state=test-state")

        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.setCookie().isNotEmpty(), "Set-Cookie header should be present")
    }

    /**
     * Test accessing a protected resource with a valid token
     */
    @Test
    fun `test accessing protected resource with valid token`() = runBlocking {
        // Create a valid token
        val token = createTestToken()

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $token")
        }

        // Verify that the response is successful
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Test accessing a protected resource without a token
     */
    @Test
    fun `test accessing protected resource without token`() = runBlocking {
        // Access a protected resource without a token
        val response = client.get("/hello")

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test accessing a protected resource with an invalid token
     */
    @Test
    fun `test accessing protected resource with invalid token`() = runBlocking {
        // Use an invalid token
        val invalidToken = "invalid.token.format"

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $invalidToken")
        }

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test accessing a protected resource with an expired token
     */
    @Test
    fun `test accessing protected resource with expired token`() = runBlocking {
        // Create an expired token
        val expiredToken = createExpiredTestToken()

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $expiredToken")
        }

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test that the user info is correctly extracted from the token
     */
    @Test
    fun `test user info extraction from token`() = runBlocking {
        // Create a token with specific claims
        val testSubject = "test-subject-789"
        val testName = "Alex Johnson"
        val testEmail = "alex.johnson@example.com"

        val claims = mapOf(
            "name" to testName,
            "email" to testEmail
        )

        val token = createTestToken(
            subject = testSubject,
            claims = claims
        )

        // Access a protected resource that returns user info
        val response = client.get("/hello") {
            header("Authorization", "Bearer $token")
        }

        // Verify that the response is successful
        assertEquals(HttpStatusCode.OK, response.status)

        // Parse the response body
        val responseBody = response.bodyAsText()
        val userInfo = Json.parseToJsonElement(responseBody).jsonObject

        // Verify that the user info contains the expected claims
        assertEquals(testSubject, userInfo["subject"]?.toString()?.trim('"'))
        assertEquals(testName, userInfo["name"]?.toString()?.trim('"'))
        assertEquals(testEmail, userInfo["email"]?.toString()?.trim('"'))
    }
}