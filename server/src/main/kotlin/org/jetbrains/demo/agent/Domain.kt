package org.jetbrains.demo.agent

import ai.koog.agents.core.tools.ToolResult
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlin.time.ExperimentalTime

/**
 * Represents events emitted by the agent during its execution.
 * These events provide information about tool calls, their results, and agent completion.
 */
@Serializable
sealed interface Domain {
    val timestamp: Instant
    val description: String

    @Serializable
    sealed interface ToolCall : Domain {
        val name: String
    }

    /**
     * Event emitted when the Weather tool is called.
     */
    @Serializable
    @SerialName("weather_tool_call")
    data class WeatherToolCall(
        override val name: String = "getWeather",
        val longitude: Double?,
        val latitude: Double?,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Fetching weather data"
    ) : Domain.ToolCall

    /**
     * Event emitted when a Google Maps Directions tool is called.
     */
    @Serializable
    @SerialName("maps_directions_tool_call")
    data class MapsToolCall(
        override val name: String = "maps_directions",
        val arguments: JsonObject?,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String
    ) : Domain.ToolCall

    /**
     * Event emitted when a generic tool is called (for tools not specifically modeled).
     */
    @Serializable
    @SerialName("generic_tool_call")
    data class GenericToolCall(
        override val name: String,
        val parameters: Map<String, String> = emptyMap(),
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Calling tool: $name"
    ) : Domain.ToolCall

    /**
     * Base class for all tool call result events.
     */
    @Serializable
    sealed interface ToolCallResult : Domain {
        val name: String
        val result: String?
    }

    /**
     * Base class for Weather tool result events.
     */
    @Serializable
    sealed interface WeatherToolResult : Domain.ToolCallResult

    /**
     * Event emitted when the Weather tool returns a successful result.
     */
    @OptIn(ExperimentalTime::class)
    @Serializable
    @SerialName("weather_tool_success_result")
    data class WeatherToolSuccessResult(
        override val name: String = "getWeather",
        val longitude: Double?,
        val latitude: Double?,
        val currentWeather: WeatherTool.CurrentWeather,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Received weather data for lat: $latitude lng: $longitude."
    ) : Domain.WeatherToolResult {
        override val result: String?
            get() = currentWeather.toStringDefault()
    }

    /**
     * Event emitted when the Weather tool call fails.
     */
    @Serializable
    @SerialName("weather_tool_failure_result")
    data class WeatherToolFailureResult(
        override val name: String = "getWeather",
        val longitude: Double?,
        val latitude: Double?,
        override val result: String,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Failed to get weather data for lat: $latitude lng: $longitude: $result"
    ) : Domain.WeatherToolResult

    /**
     * Event emitted when a Google Maps tool returns a result.
     */
    @Serializable
    @SerialName("maps_tool_result")
    data class MapsToolResult(
        override val name: String,
        override val result: String?,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Received result from Google Maps $name tool"
    ) : Domain.ToolCallResult {
        constructor(name: String, result: ToolResult?) : this(name, result?.toStringDefault())
    }

    /**
     * Event emitted when a generic tool returns a result (for tools not specifically modeled).
     */
    @Serializable
    @SerialName("generic_tool_result")
    data class GenericToolResult(
        override val name: String,
        override val result: String?,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Received result from tool: $name"
    ) : Domain.ToolCallResult {
        constructor(name: String, result: ToolResult?) : this(name, result?.toStringDefault())
    }

    /**
     * Event emitted when the agent finishes its execution.
     */
    @Serializable
    @SerialName("agent_finished")
    data class AgentFinished(
        val name: String,
        val result: String?,
        override val timestamp: Instant = Clock.System.now(),
        override val description: String = "Agent '$name' has completed its task"
    ) : Domain
}