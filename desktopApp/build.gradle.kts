import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvm()

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.github.jvsena42.floresta_node.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "FlorestaNode"
            packageVersion = "1.0.0"
            description = "Floresta Bitcoin Node"
            vendor = "Floresta"

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                menuGroup = "Floresta"
                upgradeUuid = "BF6C0B9E-6E3B-4A3D-9F5E-1A2B3C4D5E6F"
            }
            macOS {
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }
        }
    }
}
