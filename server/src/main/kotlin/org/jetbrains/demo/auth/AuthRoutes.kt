package org.jetbrains.demo.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import org.jetbrains.demo.auth.AuthRequest
import org.jetbrains.demo.auth.AuthResponse
import org.jetbrains.demo.auth.UserInfo
import com.auth0.jwt.JWT
import com.auth0.jwt.exceptions.JWTVerificationException
import com.auth0.jwk.JwkProviderBuilder
import java.net.URL
import java.util.concurrent.TimeUnit

fun Application.configureAuthRoutes() {
    routing {
        authenticate("google-jwt") {
            get("/protected") {
                val principal = call.principal<JWTPrincipal>()
                val userId = principal?.payload?.subject
                val email = principal?.payload?.getClaim("email")?.asString()

                call.respond(
                    mapOf(
                        "message" to "Access granted",
                        "userId" to userId,
                        "email" to email
                    )
                )
            }

            get("/user/profile") {
                val principal = call.principal<JWTPrincipal>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val payload = principal.payload

                val userInfo = UserInfo(
                    email = payload.getClaim("email")?.asString() ?: "",
                    picture = payload.getClaim("picture")?.asString()
                )
                call.respond(userInfo)
            }
        }
    }
}
