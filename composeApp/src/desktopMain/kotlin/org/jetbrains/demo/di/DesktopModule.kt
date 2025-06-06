package org.jetbrains.demo.di

import org.jetbrains.demo.auth.DesktopTokenProvider
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.network.TokenProvider
import org.koin.dsl.module

val desktopModule = module {
    single<TokenProvider> { DesktopTokenProvider(get<AppConfig>()) }
}