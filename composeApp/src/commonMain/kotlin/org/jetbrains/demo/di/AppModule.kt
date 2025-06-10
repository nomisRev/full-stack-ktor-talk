package org.jetbrains.demo.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import org.jetbrains.demo.auth.AuthViewModel
import org.jetbrains.demo.chat.ChatViewModel
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.network.HttpClient
import org.jetbrains.demo.auth.TokenProvider
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {
    single<Logger> { Logger(config = StaticConfig(minSeverity = Severity.Debug)) }
    singleOf(::AppConfig)
    singleOf(::HttpClient)
    factoryOf(::AuthViewModel)
    factoryOf(::ChatViewModel)
}
