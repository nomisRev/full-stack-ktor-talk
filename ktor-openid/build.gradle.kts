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
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.sessions)
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.client.contentNegotiation)
    implementation(ktorLibs.client.cio)

    testImplementation(ktorLibs.server.cio)
    testImplementation(ktorLibs.server.testHost)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.mock.oauth2.server)
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
