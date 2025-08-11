package org.jetbrains.demo.agent.chat

import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.ktor.Koog
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.*
import ai.koog.prompt.markdown.markdown
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import org.jetbrains.demo.AppConfig
import org.jetbrains.demo.agent.ktor.SSEAgent
import org.jetbrains.demo.agent.ktor.sseAgent
import org.jetbrains.demo.agent.ktor.withSystemPrompt
import org.jetbrains.demo.agent.tools

fun Application.agent(config: AppConfig) {
    val deferredTools = async { tools(config) }

    routing {
        authenticate("google") {
            sse("/chat") {
                val userQuestion = call.request.queryParameters.getOrFail("question")
                sseAgent(
                    chatAgentStrategy(),
                    OpenAIModels.CostOptimized.GPT4oMini,
                    deferredTools.await(),
                    configureAgent = { it.withSystemPrompt(system()) }
                ).run(userQuestion)
                    .filter { it is SSEAgent.Event.Agent || it is SSEAgent.Event.Tool }
                    .collect {
                        println(it)
                        send(data = it.toString())
                    }
            }
        }
    }
}

private fun system(): Prompt = prompt("travel-assistant-agent") {
    system(markdown {
        +"You're an expert travel assistant helping users reach their destination in a reliable way."
        header(1, "Task description:")
        +"You can only call tools. Figure out the accurate information from calling the google-maps tool, and the weather tool."
    })
}
