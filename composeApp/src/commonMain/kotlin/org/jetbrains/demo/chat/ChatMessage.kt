package org.jetbrains.demo.chat

data class ChatMessage(
    val text: String,
    val isFromUser: Boolean,
    val isStreaming: Boolean = false
)

data class ChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false
)