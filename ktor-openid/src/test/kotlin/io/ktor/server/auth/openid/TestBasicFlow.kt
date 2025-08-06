package io.ktor.server.auth.openid

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for JWT validation and authentication
 */
class TestBasicFlow : OpenIdConnectTestBase() {

    /**
     * Test successful JWT authentication (happy path)
     */
    @Test
    fun `test successful JWT authentication`() = runBlocking {
        // Create a valid token
        val token = createTestToken()

        // Access a protected resource
        val response =
            client.get("/hello") {
                header("Authorization", "Bearer $token")
            }

        // Verify that the response is successful
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Test JWT authentication with an invalid token format
     */
    @Test
    fun `test JWT authentication with invalid token format`() = runBlocking {
        // Create a malformed token
        val invalidToken = createMalformedToken()

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $invalidToken")
        }

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test JWT authentication with an expired token
     */
    @Test
    fun `test JWT authentication with expired token`() = runBlocking {
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
     * Test JWT authentication with wrong issuer
     */
    @Test
    fun `test JWT authentication with wrong issuer`() = runBlocking {
        // Create a token with a different issuer
        val wrongIssuerToken = createTokenWithIssuer(
            issuer = "wrong-issuer"
        )

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $wrongIssuerToken")
        }

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test JWT authentication with missing claims
     */
    @Test
    fun `test JWT authentication with missing claims`() = runBlocking {
        // Create a token with missing claims
        val tokenWithMissingClaims = createTokenWithMissingClaims(
            missingClaims = listOf("name", "email")
        )

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $tokenWithMissingClaims")
        }

        // The request should still succeed because these claims are optional
        assertEquals(HttpStatusCode.OK, response.status)

        // But the response should not contain the missing claims
        val responseBody = response.bodyAsText()
        val userInfo = Json.parseToJsonElement(responseBody).jsonObject

        assertTrue(userInfo["name"] == null || userInfo["name"].toString() == "null", "Name should be null")
        assertTrue(userInfo["email"] == null || userInfo["email"].toString() == "null", "Email should be null")
    }

    /**
     * Test JWT authentication with invalid authorization header format
     */
    @Test
    fun `test JWT authentication with invalid authorization header format`() = runBlocking {
        // Create a valid token but use an invalid header format
        val token = createTestToken()

        // Access a protected resource with invalid header format
        val response = client.get("/hello") {
            header("Authorization", "NotBearer $token")
        }

        // Verify that the response is unauthorized
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Test JWT authentication with multiple audiences including the correct one
     */
    @Test
    fun `test JWT authentication with multiple audiences including correct one`() = runBlocking {
        // Create a token with multiple audiences including the correct one
        val multiAudienceToken = createTokenWithAudience(
            audience = listOf("other-audience", testClientId, "another-audience")
        )

        // Access a protected resource
        val response = client.get("/hello") {
            header("Authorization", "Bearer $multiAudienceToken")
        }

        // Verify that the response is successful
        assertEquals(HttpStatusCode.OK, response.status)
    }

    /**
     * Test that the user info is correctly extracted from the JWT
     */
    @Test
    fun `test user info extraction from JWT`() = runBlocking {
        // Create a token with specific claims
        val testSubject = "test-subject-123"
        val testName = "John Doe"
        val testEmail = "john.doe@example.com"

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