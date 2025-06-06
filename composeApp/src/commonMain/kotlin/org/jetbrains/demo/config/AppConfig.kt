package org.jetbrains.demo.config

/**
 * Common interface for application configuration.
 * Platform-specific implementations provide the actual values.
 */
interface AppConfig {
    val googleClientId: String
    val apiBaseUrl: String
}

/**
 * Expect declaration for platform-specific configuration.
 */
expect fun AppConfig() : AppConfig