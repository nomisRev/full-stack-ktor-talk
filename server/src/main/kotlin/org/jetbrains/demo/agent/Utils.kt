package org.jetbrains.demo.agent


import ai.koog.agents.core.agent.entity.AIAgentNodeBase
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.agent.entity.AIAgentSubgraph
import ai.koog.agents.core.dsl.builder.AIAgentNodeBuilder
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.ext.agent.chatAgentStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import kotlin.reflect.typeOf

inline infix fun <reified A, B, reified C> AIAgentNodeBase<A, B>.then(next: AIAgentStrategy<B, C>): AIAgentStrategy<A, C> =
    strategy("${this.name} then ${next.name}") {
        this@then.then(next)
    }

inline fun <reified A> serialisedInputStrategy(serializer: KSerializer<A> = serializer()): AIAgentNodeBase<A, String> =
    AIAgentNodeBuilder<A, String>(
        typeOf<A>(),
        typeOf<String>()
    ) { form -> Json.encodeToString(serializer, form) }.apply {
        name = "serialisedInputStrategy"
    }.build()
