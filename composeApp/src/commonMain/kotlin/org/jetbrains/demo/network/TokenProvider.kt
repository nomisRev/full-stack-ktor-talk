package org.jetbrains.demo.network

/**
 * Interface for providing authentication tokens to the HTTP client.
 * This abstraction allows different platforms to implement token storage differently.
 */
interface TokenProvider {
    /**
     * Gets the current authentication token.
     * @return The current token or null if no token is available
     */
    suspend fun getToken(): String?
    
    /**
     * Refreshes the authentication token.
     * @return The new token or null if refresh failed
     */
    suspend fun refreshToken(): String?
    
    /**
     * Clears the stored authentication token.
     */
    suspend fun clearToken()
}