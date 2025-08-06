package io.ktor.server.auth.openid

import io.ktor.client.HttpClient
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

suspend fun Application.module(client: HttpClient) {
    install(OpenIdConnect) {
        httpClient = client
    }

    routing {
        authenticate("openid-connect-jwk") {
            get("/hello") {
                val userInfo = call.principal<OpenIdConnectPrincipal.UserInfo>()
                call.respondText(Json.encodeToString(userInfo))
            }
        }

        // Route to refresh a token
//        post("/refresh") {
//            try {
//                val parameters = call.receiveParameters()
//                val refreshToken = parameters["refresh_token"]
//                    ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing refresh_token parameter")
//
//                val newPrincipal = openIdConfig.refreshToken(refreshToken)
//                call.respond(
//                    HttpStatusCode.OK,
//                    mapOf(
//                        "id_token" to newPrincipal.idToken,
//                        "refresh_token" to newPrincipal.refreshToken,
//                        "user_info" to newPrincipal.userInfo
//                    )
//                )
//            } catch (e: Exception) {
//                call.respond(
//                    HttpStatusCode.BadRequest,
//                    "Failed to refresh token: ${e.message}"
//                )
//            }
//        }
    }
}

@Serializable
data class OpenIdConfig(
    val issuer: String,
    val clientId: String,
    val clientSecret: String,
    val scopes: List<String>,
)
