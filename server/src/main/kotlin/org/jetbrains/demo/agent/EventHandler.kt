package org.jetbrains.demo.agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.reflect.ToolFromCallable
import ai.koog.agents.features.eventHandler.feature.EventHandler
import org.jetbrains.demo.agent.Domain.AgentFinished
import org.jetbrains.demo.agent.Domain.GenericToolCall
import org.jetbrains.demo.agent.Domain.GenericToolResult
import org.jetbrains.demo.agent.Domain.MapsToolCall
import org.jetbrains.demo.agent.Domain.MapsToolResult
import org.jetbrains.demo.agent.Domain.WeatherToolCall
import org.jetbrains.demo.agent.Domain.WeatherToolFailureResult
import org.jetbrains.demo.agent.Domain.WeatherToolSuccessResult
import org.jetbrains.demo.agent.WeatherTool.WeatherToolResult

fun ToolArgs.latitude(): Double? = fromCallable("latitude") { it as? Double }

fun ToolArgs.longitude(): Double? = fromCallable("longitude") { it as? Double }

suspend fun onToolCalled(
    tool: Tool<*, *>,
    toolArgs: ToolArgs,
    handler: suspend (Domain) -> Unit
) {
    val event = when {
        tool.name == "getWeather" -> WeatherToolCall(longitude = toolArgs.longitude(), latitude = toolArgs.latitude())
        tool.name.startsWith("maps_") -> MapsToolCall(
            name = tool.name,
            arguments = toolArgs.mcpArguments(),
            description = "Using Google Maps ${tool.name.removePrefix("maps_")} service"
        )

        else -> GenericToolCall(name = tool.name)
    }

    handler(event)
}

suspend fun onToolCallResult(
    tool: Tool<*, *>,
    toolArgs: ToolArgs,
    result: ToolResult?,
    handler: suspend (Domain) -> Unit
) {
    val event = when {
        tool.name == "getWeather" -> when (result) {
            is ToolFromCallable.Result -> when (val toolResult = result.result) {
                is WeatherToolResult -> when (toolResult) {
                    is WeatherTool.CurrentWeather -> WeatherToolSuccessResult(
                        name = tool.name,
                        longitude = toolArgs.longitude(),
                        latitude = toolArgs.latitude(),
                        currentWeather = toolResult
                    )

                    is WeatherTool.Text -> WeatherToolFailureResult(
                        name = tool.name, longitude = toolArgs.longitude(),
                        latitude = toolArgs.latitude(), result = toolResult.text,
                    )
                }

                else -> WeatherToolFailureResult(
                    name = tool.name, longitude = toolArgs.longitude(),
                    latitude = toolArgs.latitude(), result = result.toStringDefault()
                )
            }

            else -> WeatherToolFailureResult(
                name = tool.name, longitude = toolArgs.longitude(),
                latitude = toolArgs.latitude(), result = result?.toStringDefault() ?: "Unknown error"
            )
        }

        tool.name.startsWith("maps_") -> MapsToolResult(name = tool.name, result = result?.toStringDefault())
        else -> GenericToolResult(name = tool.name, result = result?.toStringDefault())
    }

    handler(event)
}

suspend fun onAgentFinished(name: String, result: Any?, handler: suspend (Domain) -> Unit): Unit =
    handler(AgentFinished(name, result?.toString()))
