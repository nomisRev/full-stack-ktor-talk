package org.jetbrains.demo

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.getAs
import io.ktor.server.config.property
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.pingPeriod
import io.ktor.server.websocket.timeout
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.demo.ai.AiConfig
import org.jetbrains.demo.ai.AiService
import org.jetbrains.demo.ai.KoogAiService
import org.jetbrains.demo.ai.installAiRoutes
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.config.database
import org.jetbrains.demo.user.ExposedUserRepository
import org.jetbrains.demo.user.UserRepository
import org.jetbrains.demo.user.userRoutes
import kotlin.time.Duration.Companion.seconds

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
    val ai: AiService = KoogAiService(config.ai)

    if (developmentMode) install(CallLogging)
    install(SSE)
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
    }

    configureJwtAuth(config.jwk)

    install(ContentNegotiation) {
        json(Json {
            encodeDefaults = true
            isLenient = true
        })
    }

    routing {
        userRoutes(userRepository)
        installAiRoutes(ai)
    }
}
