package org.jetbrains.demo.config

object DesktopConfig {
    val googleClientId: String =
        System.getProperty("GOOGLE_CLIENT_ID")
            ?: System.getenv("GOOGLE_CLIENT_ID")
            ?: throw IllegalStateException("GOOGLE_CLIENT_ID not configured")

    val clientSecret =
        System.getProperty("AUTH_CLIENT_SECRET")
            ?: System.getenv("AUTH_CLIENT_SECRET")
            ?: throw IllegalStateException("AUTH_CLIENT_SECRET not configured")
}
