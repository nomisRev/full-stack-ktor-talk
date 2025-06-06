package org.jetbrains.demo.auth

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.HttpStatusCode.Companion.Unauthorized

fun Application.configureAuthRoutes() {
    routing {
        authenticate("google-jwt") {
            get("/hello") {
                val principal = call.principal<GoogleIdToken>() ?: return@get call.respond(Unauthorized)
                call.respondText("Hello, ${principal.email}!")
            }
        }
    }
}
