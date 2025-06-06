package org.jetbrains.demo

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.config.mergeWith
import io.ktor.server.testing.*
import kotlin.test.*

class AuthTest {
    @Test
    fun testProtectedEndpointWithoutAuth() = withServer {
        val response = client.get("/hello")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testProtectedEndpointWithInvalidAuth() = withServer {
        val response = client.get("/hello") {
            header(HttpHeaders.Authorization, "Bearer invalid-token")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}

fun withServer(test: suspend ApplicationTestBuilder.() -> Unit) = testApplication {
    environment {
        config = ApplicationConfig("application.yaml")
            .mergeWith(ApplicationConfig("application-test.yaml"))
    }
    test()
}