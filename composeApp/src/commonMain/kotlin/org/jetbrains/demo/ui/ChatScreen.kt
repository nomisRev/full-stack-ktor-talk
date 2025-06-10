package org.jetbrains.demo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.sse
import io.ktor.client.request.parameter
import kotlinx.coroutines.launch
import org.jetbrains.demo.config.AppConfig
import org.koin.compose.koinInject

@Composable
fun ChatScreen(
    client: HttpClient = koinInject(),
    config: AppConfig = koinInject(),
    onSignOut: () -> Unit
) {
    Logger.app.d("ChatScreen: Displaying chat for user")

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var messageText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Chat",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Logged in.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onSignOut,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sign Out")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                MessageBubble(message = message)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Message input
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                maxLines = 3
            )

            Button(
                onClick = {
                    if (messageText.isNotBlank() && !isLoading) {
                        val userMessage = messageText
                        messages = messages + ChatMessage(userMessage, true)
                        messageText = ""
                        isLoading = true

                        scope.launch {
                            try {
                                // Add a placeholder message for the AI response
                                val aiMessageIndex = messages.size
                                messages = messages + ChatMessage("", false, isStreaming = true)

                                var aiResponse = ""
                                client.sse(
                                    urlString = "${config.apiBaseUrl}/chat",
                                    request = {
                                        parameter("message", userMessage)
                                    }
                                ) {
                                    incoming.collect { event ->
                                        event.data?.let { token ->
                                            aiResponse += "$token "
                                            // Update the AI message with accumulated response
                                            messages = messages.toMutableList().apply {
                                                set(aiMessageIndex, ChatMessage(aiResponse, false, isStreaming = true))
                                            }
                                        }
                                    }
                                }

                                // Mark streaming as complete
                                messages = messages.toMutableList().apply {
                                    set(aiMessageIndex, ChatMessage(aiResponse, false, isStreaming = false))
                                }
                            } catch (e: Exception) {
                                Logger.app.e("Error during SSE chat: ${e.message}")
                                messages = messages + ChatMessage("Error: ${e.message}", false)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = messageText.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isLoading) "Sending..." else "Send")
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isFromUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isFromUser) 16.dp else 4.dp,
                bottomEnd = if (message.isFromUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (message.text.isEmpty() && message.isStreaming) "..." else message.text,
                    modifier = Modifier.weight(1f),
                    color = if (message.isFromUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                if (message.isStreaming && message.text.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 1.dp,
                        color = if (message.isFromUser) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false
)
