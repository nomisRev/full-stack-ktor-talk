@file:JvmName("Application")

package org.jetbrains.demo

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.config.property
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.sse.ServerSentEvent
import kotlinx.serialization.Serializable
import org.jetbrains.demo.ai.AiService
import org.jetbrains.demo.ai.KoogAiService
import org.jetbrains.demo.auth.*

@Serializable
data class AiConfig(val apiKey: String)

suspend fun Application.module() {
    install(ContentNegotiation) { json() }
    configureJwtAuth(property("app.jwk"))
    configureAuthRoutes()
    install(SSE)
    val ai: AiService = KoogAiService(property<AiConfig>("app.ai"))

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
        authenticate("google-jwt") {
            sse("/chat") {
                val message = call.request.queryParameters.getOrFail("message")
                ai.askQuestion(message)
                    .collect { token ->
                        send(ServerSentEvent(token))
                    }
            }
        }
    }
}