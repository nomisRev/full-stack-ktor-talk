package org.jetbrains.demo.config

import kotlinx.serialization.Serializable
import org.jetbrains.demo.ai.AiConfig
import org.jetbrains.demo.auth.JwkConfig

@Serializable
data class AppConfig(
    val host: String,
    val port: Int,
    val jwk: JwkConfig,
    val ai: AiConfig,
    val database: DatabaseConfig,
)
