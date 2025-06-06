package org.jetbrains.demo.di

import org.jetbrains.demo.auth.AuthViewModel
import org.jetbrains.demo.config.AppConfig
import org.jetbrains.demo.network.HttpClient
import org.jetbrains.demo.auth.TokenProvider
import org.koin.dsl.module

val appModule = module {
    single<AppConfig> { AppConfig() }
    single { HttpClient(get<TokenProvider>()) }
    factory { AuthViewModel(get()) }
}