plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "org.jetbrains.demo"
version = "1.0.0"
application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.logback)
    implementation(ktor.server.netty)
    implementation(ktor.server.config.yaml)
    implementation(ktor.server.auth.jwt)
    implementation(ktor.server.callLogging)
    implementation(ktor.server.contentNegotiation)
    implementation(ktor.serialization.kotlinx.json)
    implementation(ktor.client.cio)
    implementation(ktor.client.contentNegotiation)
    implementation(libs.koog.agents)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.flyway.postgresql)
    testImplementation(ktor.server.testHost)
    testImplementation(libs.kotlin.test.junit)
}