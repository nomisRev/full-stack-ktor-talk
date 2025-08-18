package org.jetbrains.demo.agent


import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.agent.entity.AIAgentSubgraph
import ai.koog.agents.core.dsl.builder.AIAgentNodeBuilder
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.ext.agent.chatAgentStrategy
import ai.koog.prompt.dsl.PromptBuilder
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.message.Message
import ai.koog.prompt.structure.StructuredData
import ai.koog.prompt.structure.StructuredResponse
import ai.koog.prompt.structure.json.JsonStructuredData
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.jetbrains.demo.AgentEvent
import org.jetbrains.demo.agent.chat.PointOfInterestFindings
import kotlin.reflect.typeOf

inline infix fun <reified A, B, reified C> AIAgentNodeBase<A, B>.then(next: AIAgentNodeBase<B, C>): AIAgentStrategy<A, C> =
    strategy("${this.name} then ${next.name}") {
        this@then.then(next)
    }

inline fun <reified A> typedInput(noinline builder: PromptBuilder.(A) -> Unit): AIAgentNodeBase<A, Message.Response> =
    AIAgentNodeBuilder<A, Message.Response>(
        typeOf<A>(),
        typeOf<Message.Response>()
    ) { value ->
        llm.writeSession {
            updatePrompt { builder(value) }
            requestLLM()
        }
    }.apply {
        name = "typedInput"
    }.build()


inline fun <reified A> structuredOutput(
    structure: StructuredData<A> = JsonStructuredData.createJsonStructure<A>(),
    retries: Int = 5,
    fixingModel: LLModel = OpenAIModels.Reasoning.O1
): AIAgentNodeBase<String, Result<StructuredResponse<A>>> =
    AIAgentNodeBuilder<String, Result<StructuredResponse<A>>>(
        typeOf<A>(),
        typeOf<Result<StructuredResponse<A>>>()
    ) { message ->
        llm.writeSession {
            updatePrompt { user(message) }
            requestLLMStructured(structure, retries, fixingModel)
        }
    }.apply {
        name = "structuredOutput"
    }.build()
