import com.android.build.api.variant.impl.VariantOutputImpl
import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

val appVersionName = "0.12.0"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.detekt)
}

android {
    namespace = "com.github.jvsena42.mandacaru"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.jvsena42.mandacaru"
        minSdk = 29
        targetSdk = 36
        versionCode = 28
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        val keystoreFile = localProperties["KEYSTORE_FILE"] as? String
        if (keystoreFile != null) {
            create("release") {
                storeFile = file(keystoreFile)
                storePassword = localProperties["KEYSTORE_PASSWORD"] as String
                keyAlias = localProperties["KEY_ALIAS"] as String
                keyPassword = localProperties["KEY_PASSWORD"] as String
            }
        }
    }

    buildTypes {
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        lintConfig = file("lint.xml")
    }
}

androidComponents {
    onVariants { variant ->
        // AGP 9 removed the legacy applicationVariants output API; the APK file
        // name is now set through the internal VariantOutputImpl.
        variant.outputs.forEach { output ->
            (output as? VariantOutputImpl)?.outputFileName?.set("Mandacaru-$appVersionName.apk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
    }
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom("$rootDir/config/detekt/detekt.yml")
    parallel = true
    source.setFrom(
        "src/main/java/com/github/jvsena42/mandacaru"
    )
}

dependencies {
    detektPlugins(libs.detekt.compose.rules)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.compose.navigation)
    implementation(libs.gson)
    implementation(libs.jna) { artifact { type = "aar" } }

    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    implementation(libs.hummingbird)
    implementation(libs.bdk.android)

    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.viewmodel)
    implementation(libs.koin.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.test)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}