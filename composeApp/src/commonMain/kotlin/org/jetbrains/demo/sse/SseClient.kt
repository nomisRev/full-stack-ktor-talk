package org.jetbrains.demo.sse

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.demo.logging.Logger

/**
 * Simple Server-Sent Events client implementation using Ktor HTTP client.
 */
data class ServerSentEvent(
    val data: String? = null,
    val event: String? = null,
    val id: String? = null,
    val retry: Long? = null
)

/**
 * Extension function to handle SSE connections with Ktor HttpClient.
 */
suspend fun HttpClient.sse(
    urlString: String,
    request: HttpRequestBuilder.() -> Unit = {},
    block: suspend (Flow<ServerSentEvent>) -> Unit
) {
    val response = prepareGet(urlString) {
        header(HttpHeaders.Accept, "text/event-stream")
        header(HttpHeaders.CacheControl, "no-cache")
        request()
    }.execute()

    if (response.status.isSuccess()) {
        val eventFlow = response.bodyAsChannel().toSseFlow()
        block(eventFlow)
    } else {
        Logger.network.e("SSE connection failed with status: ${response.status}")
        throw Exception("SSE connection failed with status: ${response.status}")
    }
}

/**
 * Converts a ByteReadChannel to a Flow of ServerSentEvent objects.
 */
private suspend fun ByteReadChannel.toSseFlow(): Flow<ServerSentEvent> = flow {
    val buffer = StringBuilder()
    
    while (!isClosedForRead) {
        try {
            val line = readUTF8Line()
            if (line == null) {
                // End of stream
                break
            }
            
            if (line.isEmpty()) {
                // Empty line indicates end of event
                val event = parseEvent(buffer.toString())
                if (event != null) {
                    emit(event)
                }
                buffer.clear()
            } else {
                buffer.appendLine(line)
            }
        } catch (e: Exception) {
            Logger.network.e("Error reading SSE stream: ${e.message}")
            break
        }
    }
}

/**
 * Parses a raw SSE event string into a ServerSentEvent object.
 */
private fun parseEvent(eventData: String): ServerSentEvent? {
    if (eventData.isBlank()) return null
    
    var data: String? = null
    var event: String? = null
    var id: String? = null
    var retry: Long? = null
    
    eventData.lines().forEach { line ->
        when {
            line.startsWith("data:") -> {
                val value = line.substring(5).trimStart()
                data = if (data == null) value else "$data\n$value"
            }
            line.startsWith("event:") -> {
                event = line.substring(6).trimStart()
            }
            line.startsWith("id:") -> {
                id = line.substring(3).trimStart()
            }
            line.startsWith("retry:") -> {
                retry = line.substring(6).trimStart().toLongOrNull()
            }
        }
    }
    
    return ServerSentEvent(data = data, event = event, id = id, retry = retry)
}