import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import io.ktor.plugin.features.DockerImageRegistry.Companion.googleContainerRegistry

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(ktorLibs.plugins.ktor)
}

application {
    mainClass.set("org.jetbrains.demo.ApplicationKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

group = "org.jetbrains.demo"
version = "1.0.0"

dependencies {
    implementation(libs.logback)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.cio)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(libs.koog.agents)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.hikari)
    implementation(libs.postgresql)
    implementation(libs.flyway.postgresql)
    implementation("io.ktor:ktor-server-di:3.2.0")
    implementation("io.ktor:ktor-server-core:3.2.0")
    implementation("io.ktor:ktor-server-sse:3.2.0")
    implementation("io.ktor:ktor-server-core:3.2.0")
    implementation("io.ktor:ktor-server-websockets:3.2.0")
    implementation("io.insert-koin:koin-ktor:3.5.6")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.6")
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotlin.test.junit)
}

fun property(name: String): String {
    val properties = gradleLocalProperties(rootDir, providers)
    return System.getenv(name)
        ?: System.getProperty(name)
        ?: properties.getProperty(name, null)
        ?: error("Property $name not found")
}

ktor {
    docker {
        localImageName = "ktor-ai-example"
        imageTag = project.version.toString()
        externalRegistry =
            googleContainerRegistry(
                projectName = provider { "Droidcon Bangladesh" },
                appName = providers.environmentVariable("GCLOUD_APPNAME"),
                username = providers.environmentVariable("GCLOUD_USERNAME"),
                password = providers.environmentVariable("GCLOUD_REGISTRY_PASSWORD"),
            )
    }
    fatJar {
        allowZip64 = true
        archiveFileName.set("dc-bangladesh.jar")
    }
}
