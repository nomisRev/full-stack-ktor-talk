package org.jetbrains.demo.agent.chat

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.message.Message
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.sse.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.async
import kotlinx.serialization.json.Json
import org.jetbrains.demo.AgentEvent
import org.jetbrains.demo.AgentEvent.*
import org.jetbrains.demo.AppConfig
import org.jetbrains.demo.JourneyForm
import org.jetbrains.demo.Tool
import org.jetbrains.demo.agent.ktor.SSEAgent
import org.jetbrains.demo.agent.ktor.sseAgent
import org.jetbrains.demo.agent.ktor.withSystemPrompt
import org.jetbrains.demo.agent.tools

public fun Route.sse(
    path: String,
    method: HttpMethod = HttpMethod.Get,
    handler: suspend ServerSSESession.() -> Unit
): Route = route(path, method) { sse(handler) }

fun Application.agent(config: AppConfig) {
    val deferredTools = async { tools(config) }

    routing {
        authenticate("google", optional = developmentMode) {
            sse("/plan", HttpMethod.Post) {
                val form = call.receive<JourneyForm>()
                sseAgent(
                    planner(),
                    OpenAIModels.CostOptimized.GPT4oMini,
                    deferredTools.await(),
                    configureAgent = {
                        it.withSystemPrompt(prompt("travel-assistant-agent") {
                            system(markdown {
                                +"You're an expert travel assistant helping users reach their destination in a reliable way."
                                header(1, "Task description:")
                                +"You can only call tools. Figure out the accurate information from calling the google-maps tool, and the weather tool."
                            })
                        })
                    }
                ).run(form)
                    .collect { event: SSEAgent.Event<JourneyForm, PointOfInterestFindings> ->
                        val result = event.toDomainEventOrNull()

                        if (result != null) {
                            application.log.debug("Sending AgentEvent: $result")
                            send(data = Json.encodeToString(AgentEvent.serializer(), result))
                        } else {
                            application.log.debug("Ignoring $event")
                        }
                    }
            }
        }
    }
}

private fun SSEAgent.Event<JourneyForm, PointOfInterestFindings>.toDomainEventOrNull(): AgentEvent? {
    var inputTokens = 0
    var outputTokens = 0
    var totalTokens = 0

    return when (this) {
        is SSEAgent.Event.Agent -> when (this) {
            is SSEAgent.Event.OnAgentBeforeClose -> null
            is SSEAgent.Event.OnAgentFinished<PointOfInterestFindings> -> AgentFinished(
                agentId = this.agentId,
                runId = this.runId,
                result = this.result.toString()
            )

            is SSEAgent.Event.OnAgentRunError ->
                AgentFinished(
                    agentId = this.agentId,
                    runId = this.runId,
                    result = this.throwable.message ?: "Unknown error"
                )

            is SSEAgent.Event.OnBeforeAgentStarted<JourneyForm, PointOfInterestFindings> -> AgentStarted(
                this.context.agentId,
                this.context.runId
            )
        }

        is SSEAgent.Event.Tool -> when (this) {
            is SSEAgent.Event.OnToolCall ->
                ToolStarted(persistentListOf(Tool(this.toolCallId!!, this.tool.name)))

            is SSEAgent.Event.OnToolCallResult ->
                ToolFinished(persistentListOf(Tool(this.toolCallId!!, this.tool.name)))

            is SSEAgent.Event.OnToolCallFailure ->
                ToolFinished(persistentListOf(Tool(this.toolCallId!!, this.tool.name)))

            is SSEAgent.Event.OnToolValidationError ->
                ToolFinished(persistentListOf(Tool(this.toolCallId!!, this.tool.name)))
        }

        is SSEAgent.Event.OnAfterLLMCall -> {
            inputTokens += this.responses.sumOf { it.metaInfo.inputTokensCount ?: 0 }
            outputTokens += this.responses.sumOf { it.metaInfo.outputTokensCount ?: 0 }
            totalTokens += this.responses.sumOf { it.metaInfo.totalTokensCount ?: 0 }
            println("Input tokens: $inputTokens, output tokens: $outputTokens, total tokens: $totalTokens")
            Message(this.responses.filterIsInstance<Message.Assistant>().map { it.content })
        }

        is SSEAgent.Event.OnBeforeLLMCall,
        is SSEAgent.Event.OnAfterNode,
        is SSEAgent.Event.OnBeforeNode,
        is SSEAgent.Event.OnNodeExecutionError,
        is SSEAgent.Event.OnStrategyFinished<*, *>,
        is SSEAgent.Event.OnStrategyStarted<*, *> -> null
    }
}
