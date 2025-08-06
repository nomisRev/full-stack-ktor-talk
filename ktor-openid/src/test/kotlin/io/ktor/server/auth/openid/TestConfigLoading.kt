package io.ktor.server.auth.openid

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.testing.testApplication
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for the configuration loading functionality
 */
class TestConfigLoading : OpenIdConnectTestBase() {

    /**
     * Test handling of missing configuration
     */
    @Test
    fun testHandlingOfMissingConfiguration() = testApplication {
        // No configuration set

        // Load configuration
        val config = OpenIdConnect.Config().apply { loadConfigFromEnvironment(application) }

        // Verify that the configuration is empty
        assertTrue(config.jwks.isEmpty(), "JWK configurations should be empty")
        assertTrue(config.oauth.isEmpty(), "OAuth configurations should be empty")
    }

    /**
     * Test loading configuration from new format with flat structure
     */
    @Test
    fun testLoadingFromNewFormatWithFlatStructure() = testApplication {
        environment {
            config = MapApplicationConfig().apply {
                // Set up new format configuration with flat structure
                put("ktor.openid.google.issuer", getIssuerUrl())
            }
        }

        // Load configuration
        val config = OpenIdConnect.Config().apply { loadConfigFromEnvironment(application) }

        // Verify that the configuration was loaded correctly
        assertEquals(1, config.jwks.size, "Should have one JWK configuration")
        assertEquals(0, config.oauth.size, "Should have no OAuth configuration")

        // Verify JWK configuration
        val jwkConfig = config.jwks[getIssuerUrl()]
        assertNotNull(jwkConfig, "JWK configuration should not be null")
        assertEquals("google", jwkConfig.name, "JWK name should be 'google'")
    }

    /**
     * Test that the extracted function works correctly with programmatic configuration
     */
    @Test
    fun testProgrammaticConfiguration() {
        // Create a config with programmatic settings
        val config = OpenIdConnect.Config().apply {
            jwk(issuer = getIssuerUrl()) {
                name = "test-jwk"
            }

            oauth(
                issuer = getIssuerUrl(),
                clientId = testClientId,
                clientSecret = testClientSecret
            ) {
                name = "test-oauth"
                scopes = listOf("openid", "profile")
            }
        }

        // Verify the configuration
        assertEquals(1, config.jwks.size, "Should have one JWK configuration")
        assertEquals(1, config.oauth.size, "Should have one OAuth configuration")

        // Verify JWK configuration
        val jwkConfig = config.jwks[getIssuerUrl()]
        assertNotNull(jwkConfig, "JWK configuration should not be null")
        assertEquals("test-jwk", jwkConfig.name, "JWK name should match")

        // Verify OAuth configuration
        val oauthConfig = config.oauth[getIssuerUrl()]
        assertNotNull(oauthConfig, "OAuth configuration should not be null")

        val (clientId, clientSecret, oauthSettings) = oauthConfig
        assertEquals(testClientId, clientId, "Client ID should match")
        assertEquals(testClientSecret, clientSecret, "Client secret should match")
        assertEquals(listOf("openid", "profile"), oauthSettings.scopes, "Scopes should match")
        assertEquals("test-oauth", oauthSettings.name, "OAuth name should match")
    }
}