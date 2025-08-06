package org.jetbrains.demo

import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels.CostOptimized.GPT4oMini
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.auth.openid.OpenIdConnect
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.demo.ai.AiService
import org.jetbrains.demo.ai.KoogAiService
import org.jetbrains.demo.ai.installAiRoutes
import org.jetbrains.demo.user.ExposedUserRepository
import org.jetbrains.demo.user.UserRepository
import org.jetbrains.demo.user.userRoutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class AppConfig(
    val host: String,
    val port: Int,
    val issuer: String,
    val apiKey: String,
    val database: DatabaseConfig,
)

fun main() {
    val config = ApplicationConfig("application.yaml")
        .property("app")
        .getAs<AppConfig>()

    embeddedServer(Netty, host = config.host, port = config.port) {
        app(config)
    }.start(wait = true)
}

suspend fun Application.app(config: AppConfig) {
    val database = database(config.database)
    val userRepository: UserRepository = ExposedUserRepository(database)
    val ai: AiService = KoogAiService(OpenAILLMClient(config.apiKey), GPT4oMini)

    configure(config)
    routes(userRepository, ai)
}

private fun Application.routes(userRepository: UserRepository, ai: AiService) {
    routing {
        userRoutes(userRepository)
        installAiRoutes(ai)
    }
}

private fun Application.configure(config: AppConfig) {
    if (developmentMode) install(CallLogging)
    install(SSE)
    install(OpenIdConnect) {
        jwk(config.issuer) {
            name = "google"
        }
    }
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
    }

    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
        })
    }
}
