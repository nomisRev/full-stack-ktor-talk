import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    jvm("desktop") {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(ktorLibs.client.android)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.googleid)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.security.crypto)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.kotlinx.coroutines.swing)
                implementation(ktorLibs.server.cio)
                implementation(ktorLibs.server.auth)
                implementation(ktorLibs.client.cio)
                implementation(libs.kotlinx.html)
            }
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(ktorLibs.client.core)
            implementation(ktorLibs.client.auth)
            implementation(ktorLibs.serialization.kotlinx.json)
            implementation(ktorLibs.client.contentNegotiation)
            implementation(ktorLibs.client.logging)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
            implementation(libs.koin.compose.viewmodel.navigation)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

fun property(name: String): String {
    val properties = gradleLocalProperties(rootDir, providers)
    return System.getenv(name)
        ?: System.getProperty(name)
        ?: properties.getProperty(name, null)
        ?: error("Property $name not found")
}

android {
    namespace = "org.jetbrains"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "org.jetbrains"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        buildConfigField(
            "String",
            "GOOGLE_CLIENT_ID",
            "\"${property("GOOGLE_CLIENT_ID")}\""
        )
        buildConfigField(
            "String",
            "API_BASE_URL",
            "\"${property("API_BASE_URL")}\""
        )
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    signingConfigs {
        getByName("debug") {
            storeFile = file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "org.jetbrains.demo.MainKt"
        val properties = gradleLocalProperties(rootDir, providers)
        jvmArgs += listOf(
            "-DGOOGLE_CLIENT_ID=${System.getenv("GOOGLE_CLIENT_ID") ?: properties.getProperty("GOOGLE_CLIENT_ID", "")}",
            "-DAPI_BASE_URL=${System.getenv("API_BASE_URL") ?: properties.getProperty("API_BASE_URL", "")}"
        )
    }
}
