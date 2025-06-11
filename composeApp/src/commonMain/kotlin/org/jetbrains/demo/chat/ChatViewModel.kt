package org.jetbrains.demo.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.demo.chat.repository.ChatRepository

class ChatViewModel(
    private val chatRepository: ChatRepository,
    base: co.touchlab.kermit.Logger
) : ViewModel() {
    private val logger = base.withTag("ChatViewModel")

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    fun sendMessage(message: String) {
        if (message.isBlank() || _state.value.isLoading) return

        logger.d("ChatViewModel: Sending message")

        // Add user message to the list
        val userMessage = ChatMessage(message, true)
        updateMessages(_state.value.messages + userMessage)

        // Set loading state
        _state.value = _state.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                // Add a placeholder message for the AI response
                val aiMessageIndex = _state.value.messages.size
                updateMessages(_state.value.messages + ChatMessage("", false, isStreaming = true))

                var aiResponse = ""
                chatRepository.sendMessage(message).collect { token ->
                    aiResponse += token
                    // Update the AI message with accumulated response
                    val updatedMessages = _state.value.messages.toMutableList().apply {
                        set(aiMessageIndex, ChatMessage(aiResponse, false, isStreaming = true))
                    }
                    updateMessages(updatedMessages)
                }

                // Mark streaming as complete
                val finalMessages = _state.value.messages.toMutableList().apply {
                    set(aiMessageIndex, ChatMessage(aiResponse, false, isStreaming = false))
                }
                updateMessages(finalMessages)
            } catch (e: Exception) {
                logger.e("Error during chat: ${e.message}")
                updateMessages(_state.value.messages + ChatMessage("Error: ${e.message}", false))
            } finally {
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    private fun updateMessages(messages: List<ChatMessage>) {
        _state.value = _state.value.copy(messages = messages)
    }
}
