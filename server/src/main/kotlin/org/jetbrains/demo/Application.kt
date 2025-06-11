@file:JvmName("Application")

package org.jetbrains.demo

import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.config.property
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.routing.*
import io.ktor.server.sse.SSE
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.jetbrains.demo.ai.AiService
import org.jetbrains.demo.ai.KoogAiService
import org.jetbrains.demo.auth.*
import org.jetbrains.demo.config.database
import org.jetbrains.demo.user.UserRepository
import org.jetbrains.demo.user.userRoutes

@Serializable
data class AiConfig(val apiKey: String)

fun Application.module() {
    val database = database(property("app.database"))
    configureJwtAuth(property("app.jwk"))
    if (developmentMode) {
        install(CallLogging)
    }

    val userRepository = UserRepository(database)
    userRoutes(userRepository)

    install(SSE)
    val ai: AiService = KoogAiService(property<AiConfig>("app.ai"))
    routing {
        authenticate("google-jwt") {
            sse("/chat") {
                val message = call.request.queryParameters.getOrFail("message")
                withContext(Dispatchers.IO) {
                    ai.askQuestion(message)
                        .collect { token ->
                            send(ServerSentEvent(token))
                        }
                }
            }
        }
    }
}
