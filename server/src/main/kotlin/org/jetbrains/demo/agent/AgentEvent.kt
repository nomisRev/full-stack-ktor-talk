package org.jetbrains.demo.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.context.AIAgentContextBase
import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import kotlin.reflect.KType

sealed interface AgentEvent<out Input, out Output> {
    // TODO: naming.. I do not want `AgentEvent.AgentEvent`, but `AgentEvent.Agent`,
    //  `AgentEvent.Strategy`, etc also feel strange.
    //  Maybe `SSEAgent.Event.Agent`, `SSEAgent.Event.Strategy`, etc and make it a nested hierarchy under `SSEAgent`.
    //  So `sealed interface Event<...>` inside `SSEAgent`, and `SSEAgent.Event.Agent` inside `SSEAgent.Event`.
    sealed interface Agent<Input, Output> : AgentEvent<Input, Output>

    data class OnBeforeAgentStarted<Input, Output>(
        public val agent: AIAgent<Input, Output>,
        public val runId: String,
        public val strategy: AIAgentStrategy<Input, Output>,
        public val feature: EventHandler,
        public val context: AIAgentContextBase
    ) : Agent<Input, Output>

    data class OnAgentFinished<Output>(
        public val agentId: String,
        public val runId: String,
        public val result: Output,
        public val resultType: KType,
    ) : Agent<Nothing, Output>

    data class OnAgentRunError(
        val agentId: String,
        val runId: String,
        val throwable: Throwable
    ) : Agent<Nothing, Nothing>

    @JvmInline
    value class OnAgentBeforeClose(val agentId: String) : Agent<Nothing, Nothing>


    sealed interface Strategy<Input, Output> : AgentEvent<Input, Output>

    data class OnStrategyStarted<Input, Output>(
        public val runId: String,
        public val strategy: AIAgentStrategy<Input, Output>,
        public val feature: EventHandler
    ) : Strategy<Input, Output>

    data class OnStrategyFinished<Input, Output>(
        public val runId: String,
        public val strategy: AIAgentStrategy<Input, Output>,
        public val feature: EventHandler,
        public val result: Output,
        public val resultType: KType,
    ) : Strategy<Input, Output>

    sealed interface Node : AgentEvent<Nothing, Nothing>

    data class OnBeforeNode(
        val node: AIAgentNodeBase<*, *>,
        val context: AIAgentContextBase,
        val input: Any?,
        val inputType: KType,
    ) : Node

    data class OnAfterNode(
        val node: AIAgentNodeBase<*, *>,
        val context: AIAgentContextBase,
        val input: Any?,
        val output: Any?,
        val inputType: KType,
        val outputType: KType,
    ) : Node

    data class OnNodeExecutionError(
        val node: AIAgentNodeBase<*, *>,
        val context: AIAgentContextBase,
        val throwable: Throwable
    ) : Node

    sealed interface LLM : AgentEvent<Nothing, Nothing>

    data class OnBeforeLLMCall(
        val runId: String,
        val prompt: Prompt,
        val model: LLModel,
        val tools: List<ToolDescriptor>,
    ) : LLM

    data class OnAfterLLMCall(
        val runId: String,
        val prompt: Prompt,
        val model: LLModel,
        val tools: List<ToolDescriptor>,
        val responses: List<Message.Response>,
        val moderationResponse: ModerationResult?
    ) : LLM

    sealed interface Tool : AgentEvent<Nothing, Nothing>

    data class OnToolCall(
        val runId: String,
        val toolCallId: String?,
        val tool: ai.koog.agents.core.tools.Tool<*, *>,
        val toolArgs: ToolArgs
    ) : Tool

    data class OnToolValidationError(
        val runId: String,
        val toolCallId: String?,
        val tool: ai.koog.agents.core.tools.Tool<*, *>,
        val toolArgs: ToolArgs,
        val error: String
    ) : Tool

    data class OnToolCallFailure(
        val runId: String,
        val toolCallId: String?,
        val tool: ai.koog.agents.core.tools.Tool<*, *>,
        val toolArgs: ToolArgs,
        val throwable: Throwable
    ) : Tool

    data class OnToolCallResult(
        val runId: String,
        val toolCallId: String?,
        val tool: ai.koog.agents.core.tools.Tool<*, *>,
        val toolArgs: ToolArgs,
        val result: ToolResult?
    ) : Tool
}
