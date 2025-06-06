package org.jetbrains.demo.config

/**
 * Desktop implementation of AppConfig using system properties or environment variables.
 */
actual fun AppConfig(): AppConfig = DesktopConfig

object DesktopConfig : AppConfig {
    override val googleClientId: String =
        System.getProperty("GOOGLE_CLIENT_ID")
            ?: System.getenv("GOOGLE_CLIENT_ID")
            ?: throw IllegalStateException("GOOGLE_CLIENT_ID not configured")

    override val apiBaseUrl: String =
        System.getProperty("API_BASE_URL")
            ?: System.getenv("API_BASE_URL")
            ?: "http://localhost:8080"

    val clientSecret =
        System.getProperty("AUTH_CLIENT_SECRET")
            ?: System.getenv("AUTH_CLIENT_SECRET")
            ?: throw IllegalStateException("AUTH_CLIENT_SECRET not configured")
}
