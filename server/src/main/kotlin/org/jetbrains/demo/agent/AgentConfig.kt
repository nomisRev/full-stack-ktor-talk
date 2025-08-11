package org.jetbrains.demo.agent

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.model.PromptExecutor
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import kotlinx.serialization.json.Json
import org.jetbrains.demo.AppConfig

suspend fun McpToolRegistryProvider.fromSseTransport(url: String) =
    fromTransport(McpToolRegistryProvider.defaultSseTransport(url))

suspend fun Application.tools(config: AppConfig): ToolRegistry {
    val googleMaps = McpToolRegistryProvider.fromSseTransport("http://localhost:9011")
    val weather = ToolRegistry { tools(WeatherTool(httpClient(), config.weatherApiUrl).asTools()) }
    val allTools = googleMaps + weather
    environment.log.info(allTools.tools.joinToString(prefix = "Running with tools:") { it.name })
    return allTools
}

private fun Application.httpClient(): HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
}.closeOnStop(this)

private fun <A : AutoCloseable> A.closeOnStop(application: Application): A = apply {
    application.monitor.subscribe(ApplicationStopped) {
        application.environment.log.info("Closing ${this::class.simpleName}...")
        close()
        application.environment.log.info("Closed ${this::class.simpleName}")
    }
}