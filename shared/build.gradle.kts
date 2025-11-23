import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
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
        val commonMain by getting {
            dependencies {
                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                // Kotlinx Serialization
                implementation(libs.kotlinx.serialization.json)

                // Ktor Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Multiplatform Settings
                implementation(libs.multiplatform.settings)
                implementation(libs.multiplatform.settings.no.arg)

                // Kotlin Logging
                implementation(libs.kotlin.logging)

                // Koin
                implementation(project.dependencies.platform(libs.koin.bom))
                implementation(libs.koin.core)
                implementation(libs.koin.compose)

                // Lifecycle ViewModel
                implementation(libs.lifecycle.viewmodel.compose)

                // JNA for FFI
                implementation("net.java.dev.jna:jna:5.14.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific dependencies
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(libs.koin.android)

                // Ktor Android Engine
                implementation(libs.ktor.client.okhttp)
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies
                implementation(compose.desktop.currentOs)

                // Ktor CIO Engine for Desktop
                implementation(libs.ktor.client.cio)
            }
        }
    }
}

android {
    namespace = "com.github.jvsena42.floresta_node.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }
}
