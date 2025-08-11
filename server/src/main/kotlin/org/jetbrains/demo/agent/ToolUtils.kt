package org.jetbrains.demo.agent

import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.mcp.McpTool
import kotlinx.serialization.json.JsonObject

fun <A> ToolArgs.fromCallable(name: String, transform: (Any?) -> A): A? =
    (this as? ToolFromCallable.VarArgs)?.args
        ?.firstNotNullOf { if (it.key.name == name) transform(it.value) else null }

fun ToolArgs.mcpArguments(): JsonObject? =
    (this as? McpTool.Args)?.arguments