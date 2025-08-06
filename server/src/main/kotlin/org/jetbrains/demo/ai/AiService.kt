package org.jetbrains.demo.ai

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.LLMClient
import ai.koog.prompt.llm.LLModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

interface AiService {
    fun askQuestion(prompt: String): Flow<String>
}

class KoogAiService(
    private val llm: LLMClient,
    private val model: LLModel
) : AiService {
    @OptIn(ExperimentalUuidApi::class)
    override fun askQuestion(prompt: String): Flow<String> = flow {
        llm.executeStreaming(prompt(Uuid.random().toString()) {
            system("You're a friendly chatbot.")
            user(prompt)
        }, model).collect(this)
    }
}