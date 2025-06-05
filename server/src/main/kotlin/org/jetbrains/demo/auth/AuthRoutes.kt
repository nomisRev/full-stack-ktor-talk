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

@Serializable
data class GoogleTokenRequest(val idToken: String)

fun Application.configureAuthRoutes() {
    // Create a JWK provider for token verification
    val jwkProvider = JwkProviderBuilder(URL("https://www.googleapis.com/oauth2/v3/certs"))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    routing {
        // Endpoint to verify ID token
        post("/auth/verify") {
            val request = call.receive<AuthRequest>()

            try {
                // Verify the token
                val jwt = JWT.decode(request.idToken)

                // Verify token signature
                val jwk = jwkProvider.get(jwt.keyId)
                val algorithm = com.auth0.jwt.algorithms.Algorithm.RSA256(jwk.publicKey as java.security.interfaces.RSAPublicKey, null)
                val verifier = JWT.require(algorithm)
                    .withIssuer("https://accounts.google.com")
                    .build()

                val verified = verifier.verify(request.idToken)

                // Extract user info from token
                val subject = verified.subject
                val email = verified.getClaim("email").asString()
                val emailVerified = verified.getClaim("email_verified").asBoolean()
                val name = verified.getClaim("name")?.asString() ?: email
                val picture = verified.getClaim("picture")?.asString()

                if (subject != null && email != null && emailVerified) {
                    // Create user info
                    val userInfo = UserInfo(
                        id = subject,
                        email = email,
                        name = name,
                        picture = picture
                    )

                    call.respond(
                        AuthResponse(
                            success = true,
                            user = userInfo,
                            message = "Authentication successful"
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        AuthResponse(
                            success = false,
                            user = null,
                            message = "Invalid token: missing required claims"
                        )
                    )
                }
            } catch (e: JWTVerificationException) {
                call.respond(
                    HttpStatusCode.Unauthorized,
                    AuthResponse(
                        success = false,
                        user = null,
                        message = "Token verification failed: ${e.message}"
                    )
                )
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    AuthResponse(
                        success = false,
                        user = null,
                        message = "Error processing token: ${e.message}"
                    )
                )
            }
        }

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
                    id = payload.subject ?: "",
                    email = payload.getClaim("email")?.asString() ?: "",
                    name = payload.getClaim("name")?.asString() ?: payload.getClaim("email")?.asString() ?: "",
                    picture = payload.getClaim("picture")?.asString()
                )
                call.respond(userInfo)
            }
        }
    }
}
