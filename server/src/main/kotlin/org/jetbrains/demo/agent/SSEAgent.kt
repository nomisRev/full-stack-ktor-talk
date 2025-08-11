package org.jetbrains.demo.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.AIAgent.FeatureContext
import ai.koog.agents.core.agent.AIAgentBase
import ai.koog.agents.core.agent.config.AIAgentConfigBase
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.executor.model.PromptExecutor
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import java.lang.IllegalStateException
import kotlin.reflect.KType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class SSEAgent<Input, Output>(
    inputType: KType,
    outputType: KType,
    promptExecutor: PromptExecutor,
    strategy: AIAgentStrategy<Input, Output>,
    agentConfig: AIAgentConfigBase,
    override val id: String = Uuid.random().toString(),
    toolRegistry: ToolRegistry = ToolRegistry.EMPTY,
    clock: Clock = Clock.System,
    installFeatures: FeatureContext.() -> Unit = {},
) : AIAgentBase<Input, Flow<AgentEvent<Input, Output>>> {

    private var channel: ProducerScope<AgentEvent<Input, Output>>? = null
    private var isRunning = false
    private val runningMutex = Mutex()

    private suspend fun send(agent: AgentEvent<Input, Output>) =
        requireNotNull(channel) { "Race condition detected: SSEAgent2 is not running anymore" }
            .send(agent)

    private val agent = AIAgent(
        inputType = inputType,
        outputType = outputType,
        promptExecutor = promptExecutor,
        strategy = strategy,
        agentConfig = agentConfig,
        id = id,
        toolRegistry = toolRegistry,
        clock = clock
    ) {
        installFeatures()
        install(EventHandler) {
            onBeforeAgentStarted { ctx ->
                send(
                    AgentEvent.OnBeforeAgentStarted<Input, Output>(
                        ctx.agent as AIAgent<Input, Output>,
                        ctx.runId,
                        ctx.strategy as AIAgentStrategy<Input, Output>,
                        ctx.feature as EventHandler,
                        ctx.context
                    )
                )
            }
            onAgentFinished { ctx ->
                send(
                    AgentEvent.OnAgentFinished(
                        ctx.agentId,
                        ctx.runId,
                        ctx.result as Output,
                        ctx.resultType
                    )
                )
            }
            onAgentRunError { ctx -> send(AgentEvent.OnAgentRunError(ctx.agentId, ctx.runId, ctx.throwable)) }
            onAgentBeforeClose { ctx -> send(AgentEvent.OnAgentBeforeClose(ctx.agentId)) }

            onStrategyStarted { ctx ->
                send(
                    AgentEvent.OnStrategyStarted(
                        ctx.runId,
                        ctx.strategy as AIAgentStrategy<Input, Output>,
                        ctx.feature
                    )
                )
            }
            onStrategyFinished { ctx ->
                send(
                    AgentEvent.OnStrategyFinished(
                        ctx.runId,
                        ctx.strategy as AIAgentStrategy<Input, Output>,
                        ctx.feature,
                        ctx.result as Output,
                        ctx.resultType
                    )
                )
            }

            onBeforeNode { ctx -> send(AgentEvent.OnBeforeNode(ctx.node, ctx.context, ctx.input, ctx.inputType)) }
            onAfterNode { ctx ->
                send(
                    AgentEvent.OnAfterNode(
                        ctx.node,
                        ctx.context,
                        ctx.input,
                        ctx.output,
                        ctx.inputType,
                        ctx.outputType
                    )
                )
            }
            onNodeExecutionError { ctx -> send(AgentEvent.OnNodeExecutionError(ctx.node, ctx.context, ctx.throwable)) }

            onBeforeLLMCall { ctx -> send(AgentEvent.OnBeforeLLMCall(ctx.runId, ctx.prompt, ctx.model, ctx.tools)) }
            onAfterLLMCall { ctx ->
                send(
                    AgentEvent.OnAfterLLMCall(
                        ctx.runId,
                        ctx.prompt,
                        ctx.model,
                        ctx.tools,
                        ctx.responses,
                        ctx.moderationResponse
                    )
                )
            }

            onToolCall { ctx -> send(AgentEvent.OnToolCall(ctx.runId, ctx.toolCallId, ctx.tool, ctx.toolArgs)) }
            onToolValidationError { ctx ->
                send(
                    AgentEvent.OnToolValidationError(
                        ctx.runId,
                        ctx.toolCallId,
                        ctx.tool,
                        ctx.toolArgs,
                        ctx.error
                    )
                )
            }
            onToolCallFailure { ctx ->
                send(
                    AgentEvent.OnToolCallFailure(
                        ctx.runId,
                        ctx.toolCallId,
                        ctx.tool,
                        ctx.toolArgs,
                        ctx.throwable
                    )
                )
            }
            onToolCallResult { ctx ->
                send(
                    AgentEvent.OnToolCallResult(
                        ctx.runId,
                        ctx.toolCallId,
                        ctx.tool,
                        ctx.toolArgs,
                        ctx.result
                    )
                )
            }
        }
    }

    override suspend fun run(agentInput: Input): Flow<AgentEvent<Input, Output>> =
        channelFlow {
            runningMutex.withLock {
                if (isRunning) {
                    throw IllegalStateException("Agent is already running")
                }

                isRunning = true
            }
            this@SSEAgent.channel = this
            agent.run(agentInput)
            this@SSEAgent.channel = null
            runningMutex.withLock { isRunning = false }
        }
}