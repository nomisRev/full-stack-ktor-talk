package org.jetbrains.demo.agent

import ai.koog.agents.core.agent.AIAgent.FeatureContext
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.ktor.Koog
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.*
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.markdown.markdown
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.pluginOrNull
import io.ktor.server.routing.routing
import io.ktor.server.sse.ServerSSESession
import io.ktor.server.sse.sse
import io.ktor.server.util.getOrFail
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.Clock
import org.jetbrains.demo.AppConfig
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private val logger = KotlinLogging.logger {}

// TODO bring to koog-ktor
public suspend fun <Input, Output> ServerSSESession.sseAgent(
    inputType: KType,
    outputType: KType,
    strategy: AIAgentStrategy<Input, Output>,
    model: LLModel,
    tools: ToolRegistry = ToolRegistry.EMPTY,
    clock: Clock = Clock.System,
    // TODO We need to create a proper `AgentConfig` builder inside of Ktor that allows overriding global configuration.
    configureAgent: (AIAgentConfig) -> (AIAgentConfig) = { it },
    installFeatures: FeatureContext.() -> Unit = {}
): SSEAgent<Input, Output> {
    val plugin = requireNotNull(call.application.pluginOrNull(Koog)) { "Plugin $Koog is not configured" }

    @Suppress("invisible_reference", "invisible_member")
    return SSEAgent(
        inputType = inputType,
        outputType = outputType,
        promptExecutor = plugin.promptExecutor,
        strategy = strategy,
        agentConfig = plugin.agentConfig(model).let(configureAgent),
        toolRegistry = plugin.agentConfig.toolRegistry + tools,
        clock = clock,
        installFeatures = installFeatures
    )
}

public suspend inline fun <reified Input, reified Output> ServerSSESession.sseAgent(
    strategy: AIAgentStrategy<Input, Output>,
    model: LLModel,
    tools: ToolRegistry = ToolRegistry.EMPTY,
    clock: Clock = Clock.System,
    noinline configureAgent: (AIAgentConfig) -> (AIAgentConfig) = { it },
    noinline installFeatures: FeatureContext.() -> Unit = {}
): SSEAgent<Input, Output> = sseAgent(
    typeOf<Input>(),
    typeOf<Output>(),
    strategy,
    model,
    tools,
    clock,
    configureAgent,
    installFeatures
)

fun Application.events(config: AppConfig) {
    val deferredTools = async { tools(config) }
    routing {
        // TODO session auth
        sse("/plan") {
            val userQuestion = call.request.queryParameters.getOrFail("question")
            sseAgent(
                chatAgentStrategy(),
                OpenAIModels.CostOptimized.GPT4oMini,
                deferredTools.await(),
                configureAgent = { it.withSystemPrompt(system()) }
            ).run(userQuestion)
                .filter { it is AgentEvent.Agent || it is AgentEvent.Tool }
                .collect {
                    println(it)
                    send(data = it.toString())
                }
        }
    }
}

private fun AIAgentConfig.withSystemPrompt(prompt: Prompt): AIAgentConfig =
    AIAgentConfig(prompt, model, maxAgentIterations, missingToolsConversionStrategy)

private fun system(): Prompt = prompt("travel-assistant-agent") {
    system(markdown {
        +"You're an expert travel assistant helping users reach their destination in a reliable way."
        header(1, "Task description:")
        +"You can only call tools. Figure out the accurate information from calling the google-maps tool, and the weather tool."
    })
}
