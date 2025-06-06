@file:JvmName("Application")

package org.jetbrains.demo

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.config.property
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.demo.auth.*

suspend fun Application.module() {
    install(ContentNegotiation) { json() }
    configureJwtAuth(property("app.jwk"))
    configureAuthRoutes()

    routing {
        get("/") {
            call.respondText("Ktor: ${Greeting().greet()}")
        }
    }
}