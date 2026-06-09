import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    val xcf = XCFramework("shared")

    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
            xcf.add(this)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.littlemandarin.classics.shared"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
    }

    sourceSets {
        getByName("main") {
            resources.srcDir("src/commonMain/resources")
        }
        getByName("test") {
            resources.srcDir("src/commonMain/resources")
        }
    }
}

val xcodeAvailable = try {
    ProcessBuilder("xcrun", "xcodebuild", "-version")
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start()
        .waitFor() == 0
} catch (_: Exception) {
    false
}

tasks.configureEach {
    val isIosTask = name.contains("ios", ignoreCase = true)
    val isXcFrameworkTask = name.contains("XCFramework", ignoreCase = true)
    if (isIosTask || isXcFrameworkTask) {
        onlyIf("Xcode is available for iOS Kotlin/Native tasks") {
            xcodeAvailable
        }
    }
}
