package org.jetbrains.demo.ai

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Serializable
data class AiConfig(val apiKey: String)

interface AiService {
    fun askQuestion(prompt: String): Flow<String>
}

class KoogAiService(
    private val llm: LLMClient,
    private val model: LLModel = OpenAIModels.CostOptimized.GPT4oMini
) : AiService {
    constructor(
        config: AiConfig,
        model: LLModel = OpenAIModels.CostOptimized.GPT4oMini
    ) : this(OpenAILLMClient(config.apiKey), model)

    @OptIn(ExperimentalUuidApi::class)
    override fun askQuestion(prompt: String): Flow<String> = flow {
        llm.executeStreaming(prompt(Uuid.random().toString()) {
            system("You're a friendly chatbot.")
            user(prompt)
        }, model).collect(this)
    }
}