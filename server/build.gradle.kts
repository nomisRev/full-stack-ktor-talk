plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
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
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.koog.agents)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.flyway.postgresql)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}