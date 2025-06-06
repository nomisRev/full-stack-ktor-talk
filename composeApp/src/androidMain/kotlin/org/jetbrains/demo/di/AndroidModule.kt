package org.jetbrains.demo.di

import org.jetbrains.demo.auth.AndroidTokenProvider
import org.jetbrains.demo.network.TokenProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single<TokenProvider> { AndroidTokenProvider(androidContext(), get()) }
}