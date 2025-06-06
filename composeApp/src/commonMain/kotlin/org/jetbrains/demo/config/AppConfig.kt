package org.jetbrains.demo.config

interface AppConfig {
    val googleClientId: String
    val apiBaseUrl: String
}

expect fun AppConfig(): AppConfig
