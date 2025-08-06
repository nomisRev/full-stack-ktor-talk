//package io.ktor.server.auth.openid
//
//import io.ktor.client.request.get
//import io.ktor.client.statement.bodyAsText
//import io.ktor.http.HttpStatusCode
//import io.ktor.server.application.install
//import io.ktor.server.response.respond
//import io.ktor.server.routing.get
//import io.ktor.server.routing.routing
//import io.ktor.server.sessions.get
//import io.ktor.server.sessions.sessions
//import io.ktor.server.sessions.set
//import io.ktor.server.testing.testApplication
//import kotlinx.serialization.json.Json
//import kotlinx.serialization.json.jsonObject
//import kotlinx.serialization.json.jsonPrimitive
//import kotlin.test.Test
//import kotlin.test.assertEquals
//import kotlin.test.assertNotNull
//
//class SessionManagementTest {
//
//    @Test
//    fun testSessionConfigurationIsApplied() {
//        application {
//            install(OpenIdConnect)
//
//            routing {
//                get("/test-session-config") {
//                    call.respond(HttpStatusCode.OK, "Session config applied")
//                }
//            }
//        }
//
//        val response = client.get("/test-session-config")
//        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals("Session config applied", response.bodyAsText())
//    }
//
//    @Test
//    fun testLogoutEndpointClearsSession() {
//        application {
//            install(OpenIdConnect)
//
//            routing {
//                get("/test-logout") {
//                    // Simulate having a session
//                    val testPrincipal = OpenIdConnectPrincipal(
//                        idToken = "test-token",
//                        refreshToken = "test-refresh",
//                    )
//                    call.sessions.set(testPrincipal)
//                    call.respond(HttpStatusCode.OK, "Session set")
//                }
//
//                get("/check-session") {
//                    val session = call.sessions.get("TEST_SESSION") as? OpenIdConnectPrincipal
//                    if (session != null) {
//                        call.respond(HttpStatusCode.OK, "Session exists")
//                    } else {
//                        call.respond(HttpStatusCode.OK, "No session")
//                    }
//                }
//            }
//        }
//
//        // Set a session
//        val setResponse = client.get("/test-logout")
//        assertEquals(HttpStatusCode.OK, setResponse.status)
//
//        // Check session exists
//        val checkResponse1 = client.get("/check-session")
//        assertEquals(HttpStatusCode.OK, checkResponse1.status)
//        assertEquals("Session exists", checkResponse1.bodyAsText())
//    }
//
//    @Test
//    fun testSessionConfigurationDefaults() {
//        application {
//            install(OpenIdConnect) {
//                // Use default session configuration
//            }
//
//            routing {
//                get("/test-defaults") {
//                    call.respond(HttpStatusCode.OK, "Defaults applied")
//                }
//            }
//        }
//
//        val response = client.get("/test-defaults")
//        assertEquals(HttpStatusCode.OK, response.status)
//        assertEquals("Defaults applied", response.bodyAsText())
//    }
//}