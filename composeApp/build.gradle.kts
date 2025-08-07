import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeFeatureFlag
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.hot.reload)
}

kotlin {
    jvmToolchain(21)

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
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer =
                    (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                        static =
                            (static ?: mutableListOf()).apply {
                                add(rootDirPath)
                                add(projectDirPath)
                            }
                    }
            }
            @OptIn(ExperimentalDistributionDsl::class)
            distribution { outputDirectory = file("$rootDir/server/src/main/resources/web") }
        }
        binaries.executable()
    }
    sourceSets {
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
            implementation(libs.androidx.navigation.compose)
        }
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
                implementation(project(":ktor-openid"))
                implementation(ktorLibs.server.auth)
                implementation(ktorLibs.client.cio)
                implementation(libs.kotlinx.html)
            }
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

composeCompiler {
    featureFlags.add(ComposeFeatureFlag.OptimizeNonSkippingGroups)
}

dependencies {
    debugImplementation(compose.uiTooling)
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
            buildConfigField("String", "API_BASE_URL", "\"10.0.2.2\"")
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

tasks {
    register("buildDevWebsite") {
        group = "kotlin browser"
        description = "Builds the website in development mode"
        dependsOn("wasmJsBrowserDevelopmentWebpack")
        dependsOn("wasmJsBrowserDevelopmentExecutableDistribution")
    }

    register("buildProdWebsite") {
        group = "kotlin browser"
        description = "Builds the website in production mode"
        dependsOn("wasmJsBrowserProductionWebpack")
        dependsOn("wasmJsBrowserDistribution")
    }
}
