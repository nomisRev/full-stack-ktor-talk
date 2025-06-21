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
    val jwkUrl: String
)

fun Application.configureJwtAuth(config: JwkConfig) {
    authentication {
        val jwk = JwkProviderBuilder(URL(config.jwkUrl))
            .cached(10, 24, TimeUnit.HOURS)
            .rateLimited(10, 1, TimeUnit.MINUTES)
            .build()

        jwt("google-jwt") {
            verifier(jwk, config.issuer) {
                acceptLeeway(3)
            }
            validate { cred: JWTCredential ->
                val subject: String? = cred.payload.subject
                if (subject != null) GoogleIdToken(subject, cred.payload)
                else null
            }
        }
    }
}

class GoogleIdToken(val subject: String, val payload: Payload)
