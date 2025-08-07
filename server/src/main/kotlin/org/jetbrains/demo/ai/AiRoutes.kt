package org.jetbrains.demo.ai

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Routing
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn

fun Routing.aiRoutes(ai: AiService) {
    authenticate("google") {
        sse("/chat") {
            val message = call.request.queryParameters.getOrFail("message")
            ai.askQuestion(message)
                .flowOn(Dispatchers.IO)
                .collect { token ->
                    send(ServerSentEvent(token))
                }
        }
    }
}
