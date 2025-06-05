package org.jetbrains.demo.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import java.net.URL
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

fun Application.configureJwtAuth() {
    install(Authentication) {
        jwt("google-jwt") {
            val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/oauth2/v3/certs"))
                .cached(10, 24, TimeUnit.HOURS) // Cache JWKs for 24 hours, max 10 entries
                .rateLimited(10, 1, TimeUnit.MINUTES) // Rate limit: 10 requests per minute
                .build()

            verifier(jwkProvider, "https://accounts.google.com")

            validate { credential ->
                // Validate the JWT payload
                val payload = credential.payload

                // Check if token has required claims
                val subject = payload.subject
                val email = payload.getClaim("email")?.asString()
                val emailVerified = payload.getClaim("email_verified")?.asBoolean()

                if (subject != null && email != null && emailVerified == true) {
                    JWTPrincipal(payload)
                } else {
                    null
                }
            }
        }
    }
}