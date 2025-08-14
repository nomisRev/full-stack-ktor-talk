package org.jetbrains.demo.agent.chat

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.*
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.message.Message
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.shareIn
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
) = route(path, method) { sse(handler) }

fun Application.agent(config: AppConfig) {
    val deferredTools = async { tools(config) }

    routing {
        authenticate("google") {
            sse("/plan", HttpMethod.Post) {
                val form = call.receive<JourneyForm>()
                var inputTokens = 0
                var outputTokens = 0
                var totalTokens = 0

                sseAgent(
                    planner(),
                    OpenAIModels.CostOptimized.GPT4oMini,
                    deferredTools.await(),
                    configureAgent = { it.withSystemPrompt(system()) }
                ).run(form)
                    .collect { event ->
                        val result = when (event) {
                            is SSEAgent.Event.Agent -> when (event) {
                                is SSEAgent.Event.OnAgentBeforeClose -> null
                                is SSEAgent.Event.OnAgentFinished<String> -> AgentFinished(
                                    agentId = event.agentId,
                                    runId = event.runId,
                                    result = event.result
                                )

                                is SSEAgent.Event.OnAgentRunError ->
                                    AgentFinished(
                                        agentId = event.agentId,
                                        runId = event.runId,
                                        result = event.throwable.message ?: "Unknown error"
                                    )

                                is SSEAgent.Event.OnBeforeAgentStarted<JourneyForm, String> -> AgentStarted(
                                    event.context.agentId,
                                    event.context.runId
                                )
                            }

                            is SSEAgent.Event.Tool -> when (event) {
                                is SSEAgent.Event.OnToolCall ->
                                    ToolStarted(persistentListOf(Tool(event.toolCallId!!, event.tool.name)))

                                is SSEAgent.Event.OnToolCallResult ->
                                    ToolFinished(persistentListOf(Tool(event.toolCallId!!, event.tool.name)))

                                is SSEAgent.Event.OnToolCallFailure ->
                                    ToolFinished(persistentListOf(Tool(event.toolCallId!!, event.tool.name)))

                                is SSEAgent.Event.OnToolValidationError ->
                                    ToolFinished(persistentListOf(Tool(event.toolCallId!!, event.tool.name)))
                            }

                            is SSEAgent.Event.OnAfterLLMCall -> {
                                inputTokens += event.responses.sumOf { it.metaInfo.inputTokensCount ?: 0 }
                                outputTokens += event.responses.sumOf { it.metaInfo.outputTokensCount ?: 0 }
                                totalTokens += event.responses.sumOf { it.metaInfo.totalTokensCount ?: 0 }
                                Message(event.responses.filterIsInstance<Message.Assistant>().map { it.content })
                            }

                            is SSEAgent.Event.OnBeforeLLMCall,
                            is SSEAgent.Event.OnAfterNode,
                            is SSEAgent.Event.OnBeforeNode,
                            is SSEAgent.Event.OnNodeExecutionError,
                            is SSEAgent.Event.OnStrategyFinished<*, *>,
                            is SSEAgent.Event.OnStrategyStarted<*, *> -> null
                        }

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

private fun system(): Prompt = prompt("travel-assistant-agent") {
    system(markdown {
        +"You're an expert travel assistant helping users reach their destination in a reliable way."
        header(1, "Task description:")
        +"You can only call tools. Figure out the accurate information from calling the google-maps tool, and the weather tool."
    })
}

fun planner(): AIAgentStrategy<JourneyForm, String> = strategy("travel-planner") {
    val sendInput by node<JourneyForm, Message.Response> { input ->
        llm.writeSession {
            updatePrompt {
                user(markdown {
                    header(1, "Task description")
                    +"Find points of interest that are relevant to the travel journey and travelers."
                    +"Use mapping tools to consider appropriate order and put a rough date range for each point of interest."
                    header(2, "Details")
                    bulleted {
                        item("The travelers are ${input.travelers}.")
                        item("Travelling from ${input.fromCity} to ${input.toCity}.")
                        item("Leaving on ${input.startDate}, and returning on ${input.endDate}.")
                        item("The preferred transportation method is ${input.transport}.")
                    }
                })
            }
            requestLLM()
        }
    }

    val giveFeedbackToCallTools by node<String, Message.Response> { input ->
        llm.writeSession {
            updatePrompt {
                user(markdown {
                    +"Don't chat with plain text! Call one of the available tools, instead:"
                    bulleted {
                        tools.forEach { item(it.name) }
                    }
                })
            }

            requestLLM()
        }
    }

    val executeTool by nodeExecuteTool()
    val sendToolResult by nodeLLMSendToolResult()

    edge(nodeStart forwardTo sendInput)

    edge(sendInput forwardTo executeTool onToolCall { true })
    edge(sendInput forwardTo giveFeedbackToCallTools onAssistantMessage { true })

    edge(giveFeedbackToCallTools forwardTo executeTool onToolCall { tc -> tc.tool != "__exit__" })
    edge(giveFeedbackToCallTools forwardTo nodeFinish onToolCall { tc -> tc.tool == "__exit__" } transformed { "Chat finished" })
    edge(giveFeedbackToCallTools forwardTo nodeFinish onAssistantMessage { true })

    edge(executeTool forwardTo sendToolResult)
    edge(sendToolResult forwardTo nodeFinish onAssistantMessage { true })

    edge(
        sendToolResult forwardTo nodeFinish
                onToolCall { tc -> tc.tool == "__exit__" } transformed { "Chat finished" }
    )
    edge(sendToolResult forwardTo executeTool onToolCall { true })
}
