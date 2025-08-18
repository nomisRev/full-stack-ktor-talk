package org.jetbrains.demo.agent.koog

import ai.koog.agents.core.dsl.builder.AIAgentSubgraphDelegate
import ai.koog.agents.core.dsl.builder.forwardTo

inline fun <reified A, reified O, reified A2> AIAgentSubgraphDelegate<A, O>.mapInput(noinline block: (A2) -> A): AIAgentSubgraphDelegate<A2, O> =
    subgraph {
        val transform by node<A2, A> { input: A2 -> block(input) }
        val input by this@mapInput
        edge(nodeStart forwardTo transform)
        edge(transform forwardTo input)
        edge(input forwardTo nodeFinish)
    }

inline fun <reified A, reified O, reified O2> AIAgentSubgraphDelegate<A, O>.mapOutput(noinline block: (O) -> O2): AIAgentSubgraphDelegate<A, O2> =
    subgraph {
        val transform by node<O, O2> { output: O -> block(output) }
        val output by this@mapOutput
        edge(nodeStart forwardTo output)
        edge(output forwardTo transform)
        edge(transform forwardTo nodeFinish)
    }
