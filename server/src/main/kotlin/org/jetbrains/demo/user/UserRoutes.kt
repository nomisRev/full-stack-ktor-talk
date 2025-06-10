package org.jetbrains.demo.user

import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import org.jetbrains.demo.auth.GoogleIdToken

fun Application.userRoutes(users: UserRepository) = routing {
    authenticate("google-jwt") {
        route("user") {
            install(ContentNegotiation) { json() }
            post("/create") {
                val idToken = call.principal<GoogleIdToken>() ?: return@post call.respond(HttpStatusCode.Unauthorized)
                val user = users.create(idToken.subject)
                call.respond(HttpStatusCode.OK, user)
            }

            get("/") {
                val idToken = call.principal<GoogleIdToken>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val user = users.findOrNull(idToken.subject) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, user)
            }

            get("/all") {
                val idToken = call.principal<GoogleIdToken>() ?: return@get call.respond(HttpStatusCode.Unauthorized)
                val users = users.findAll()
                call.respond(HttpStatusCode.OK, users)
            }

            get("/email/{email}") {
                val email = call.parameters["email"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                val user = users.findByEmail(email) ?: return@get call.respond(HttpStatusCode.NotFound)
                call.respond(HttpStatusCode.OK, user)
            }

            put("/update") {
                val idToken = call.principal<GoogleIdToken>() ?: return@put call.respond(HttpStatusCode.Unauthorized)
                val update = call.receive<UpdateUser>()
                val updatedUser = users.create(idToken.subject, update)
                call.respond(HttpStatusCode.OK, updatedUser)
            }

            delete("/delete") {
                val idToken = call.principal<GoogleIdToken>() ?: return@delete call.respond(HttpStatusCode.Unauthorized)
                users.delete(idToken.subject)
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}
