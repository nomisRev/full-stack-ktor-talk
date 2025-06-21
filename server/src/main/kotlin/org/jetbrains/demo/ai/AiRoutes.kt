package org.jetbrains.demo.ai

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun Routing.installAiRoutes(ai: AiService) {
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