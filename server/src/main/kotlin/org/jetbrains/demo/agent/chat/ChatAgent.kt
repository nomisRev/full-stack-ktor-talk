package org.jetbrains.demo.agent.chat

import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.*
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.message.Message
import io.ktor.http.HttpMethod
import io.ktor.http.HttpMethod.Companion
import io.ktor.server.application.Application
import io.ktor.server.application.log
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.routing.Route
import io.ktor.server.routing.application
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.ServerSSESessionWithSerialization
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import io.ktor.util.reflect.TypeInfo
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import org.jetbrains.demo.AgentEvent
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
                sseAgent(
                    planner(),
                    OpenAIModels.CostOptimized.GPT4oMini,
                    deferredTools.await(),
                    configureAgent = { it.withSystemPrompt(system()) }
                ).run(form)
                    .mapNotNull {
                        when (it) {
                            is SSEAgent.Event.Agent -> when (it) {
                                is SSEAgent.Event.OnAgentBeforeClose -> null
                                is SSEAgent.Event.OnAgentFinished<String> -> AgentEvent.AgentFinished(it.result)
                                is SSEAgent.Event.OnAgentRunError -> AgentEvent.AgentFinished(
                                    it.throwable.message ?: "Unknown error"
                                )

                                is SSEAgent.Event.OnBeforeAgentStarted<JourneyForm, String> -> AgentEvent.AgentStarted
                            }

                            is SSEAgent.Event.Tool -> when (it) {
                                is SSEAgent.Event.OnToolCall ->
                                    AgentEvent.ToolStarted(persistentListOf(Tool(it.toolCallId!!, it.tool.name)))

                                is SSEAgent.Event.OnToolCallResult ->
                                    AgentEvent.ToolFinished(persistentListOf(Tool(it.toolCallId!!, it.tool.name)))

                                is SSEAgent.Event.OnToolCallFailure ->
                                    AgentEvent.ToolFinished(persistentListOf(Tool(it.toolCallId!!, it.tool.name)))

                                is SSEAgent.Event.OnToolValidationError ->
                                    AgentEvent.ToolFinished(persistentListOf(Tool(it.toolCallId!!, it.tool.name)))
                            }

                            else -> null
                        }
                    }.collect { event ->
                        application.log.debug("Sending AgentEvent: $event")
                        send(data = Json.encodeToString(AgentEvent.serializer(), event))
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
