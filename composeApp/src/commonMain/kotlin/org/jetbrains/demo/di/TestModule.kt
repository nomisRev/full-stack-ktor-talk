package org.jetbrains.demo.di

import co.touchlab.kermit.Logger
import org.jetbrains.demo.chat.repository.ChatRepository
import org.jetbrains.demo.chat.repository.StubChatRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Koin module for testing that provides stub implementations.
 * Use this module instead of or in addition to appModule for testing.
 */
val testModule = module {
    // Use the stub repository for testing
    singleOf(::StubChatRepository) bind ChatRepository::class
}