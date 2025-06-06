package org.jetbrains.demo.logging

import co.touchlab.kermit.Logger as KermitLogger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig

object Logger {
    private val baseLogger = KermitLogger(
        config = StaticConfig(
            minSeverity = Severity.Debug
        )
    )

    // Create tag-specific loggers
    val auth = baseLogger.withTag("Auth")
    val network = baseLogger.withTag("Network")
    val app = baseLogger.withTag("App")

    // Convenience methods for logging
    fun d(tag: String, message: String, throwable: Throwable? = null) {
        baseLogger.withTag(tag).d(message, throwable)
    }

    fun i(tag: String, message: String, throwable: Throwable? = null) {
        baseLogger.withTag(tag).i(message, throwable)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        baseLogger.withTag(tag).w(message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        baseLogger.withTag(tag).e(message, throwable)
    }
}