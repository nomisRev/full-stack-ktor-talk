package org.jetbrains.demo.di

import org.jetbrains.demo.auth.DesktopTokenProvider
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.auth.TokenProvider
import org.jetbrains.demo.config.DesktopConfig
import org.koin.dsl.module

val desktopModule = module {
    single<DesktopConfig> { DesktopConfig }
    single<TokenProvider> { DesktopTokenProvider(get()) }
}