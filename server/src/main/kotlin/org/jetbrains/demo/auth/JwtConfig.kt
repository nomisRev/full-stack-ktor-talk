package org.jetbrains.demo.auth

import io.ktor.server.application.*
import io.ktor.server.auth.jwt.*

fun Application.configureJwtAuth() {
    configureJwk("https://accounts.google.com/") { credential ->
        val subject = credential.payload.subject
        val email = credential.payload.getClaim("email")?.asString()
        val emailVerified = credential.payload.getClaim("email_verified")?.asBoolean()

        if (subject != null && email != null && emailVerified == true) {
            JWTPrincipal(credential.payload)
        } else {
            null
        }
    }
}
