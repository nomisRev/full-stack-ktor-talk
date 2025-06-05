package org.jetbrains.demo

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.demo.auth.GoogleTokenRequest
import kotlin.test.*

class AuthTest {
    @Test
    fun testProtectedEndpointWithoutAuth() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/protected")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testProtectedEndpointWithInvalidAuth() = testApplication {
        application {
            module()
        }
        
        val response = client.get("/protected") {
            header(HttpHeaders.Authorization, "Bearer invalid-token")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}