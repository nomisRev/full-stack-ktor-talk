package org.jetbrains.demo.auth

import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.interfaces.Payload
import io.ktor.server.application.*
import io.ktor.server.auth.authentication
import io.ktor.server.auth.jwt.*
import kotlinx.serialization.Serializable
import java.net.URL
import java.util.concurrent.TimeUnit

@Serializable
data class JwkConfig(
    val issuer: String,
    val jwkUrl: String,
    val clientId: String
)

fun Application.configureJwtAuth(config: JwkConfig) {
    authentication {
        val jwk = JwkProviderBuilder(URL(config.jwkUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

        jwt("google-jwt") {
            authSchemes()
            verifier(jwk, config.issuer) {
                withAudience(config.clientId)
            }
            validate { credential ->
                val email = credential.payload.getClaim("email")?.asString()
                val emailVerified = credential.payload.getClaim("email_verified")?.asBoolean()

                if (email != null && emailVerified == true) {
                    GoogleIdToken(email, credential.payload)
                } else {
                    null
                }
            }
        }
    }
}

class GoogleIdToken(val email: String, payload: Payload): JWTPayloadHolder(payload)
