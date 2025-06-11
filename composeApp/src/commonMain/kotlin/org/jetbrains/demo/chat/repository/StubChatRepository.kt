package org.jetbrains.demo.chat.repository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import co.touchlab.kermit.Logger

/**
 * Stub implementation of ChatRepository for testing.
 * Returns predefined responses without making actual network calls.
 */
class StubChatRepository(
    base: Logger
) : ChatRepository {
    private val logger = base.withTag("StubChatRepository")

    override suspend fun sendMessage(message: String): Flow<String> = flow {
        logger.d("StubChatRepository: Received message: $message")
        
        // Simulate network delay
        delay(500)
        
        // Generate a simple response based on the input message
        val responseWords = generateResponse(message)
        
        // Emit each word with a delay to simulate streaming
        for (word in responseWords) {
            emit(word)
            delay(100) // Simulate token-by-token streaming
        }
    }
    
    private fun generateResponse(message: String): List<String> {
        // Simple response generation logic
        return when {
            message.contains("hello", ignoreCase = true) -> 
                "Hello! How can I help you today?".split(" ")
            
            message.contains("help", ignoreCase = true) -> 
                "I'm a stub chat repository for testing. I can respond to basic queries.".split(" ")
            
            message.contains("?") -> 
                "That's an interesting question. Let me think about it...".split(" ")
            
            else -> 
                "I received your message: \"$message\". This is a stub response for testing purposes.".split(" ")
        }
    }
}