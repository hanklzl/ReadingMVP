import java.util.Properties
import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val versionProps = Properties().also { props ->
    val versionFile = rootProject.file("version.properties")
    if (!versionFile.isFile) {
        throw GradleException("version.properties is required at ${versionFile.path}")
    }
    versionFile.inputStream().use { stream -> props.load(stream) }
}
val appVersionCode: Int = versionProps.getProperty("versionCode")?.toIntOrNull()
    ?: throw GradleException("version.properties: versionCode missing or invalid")
val appVersionName: String = versionProps.getProperty("versionName")
    ?: throw GradleException("version.properties: versionName missing")

val releaseSigningEnvironmentVariables = listOf(
    "ANDROID_RELEASE_KEYSTORE_PATH",
    "ANDROID_RELEASE_STORE_PASSWORD",
    "ANDROID_RELEASE_KEY_ALIAS",
    "ANDROID_RELEASE_KEY_PASSWORD",
)

val releaseSigningRequested = gradle.startParameter.taskNames.any { taskName ->
    val normalizedTaskName = taskName.substringAfterLast(':')
    normalizedTaskName.equals("assembleRelease", ignoreCase = true) ||
        normalizedTaskName.equals("bundleRelease", ignoreCase = true) ||
        normalizedTaskName.equals("packageRelease", ignoreCase = true) ||
        normalizedTaskName.equals("build", ignoreCase = true) ||
        normalizedTaskName.endsWith("Release", ignoreCase = true)
}

fun requiredReleaseSigningEnv(name: String): String =
    providers.environmentVariable(name).orNull
        ?: throw GradleException(
            "Missing release signing environment variable: $name. " +
                "Set ${releaseSigningEnvironmentVariables.joinToString()} before running a release build."
        )

android {
    namespace = "com.littlemandarin.classics"
    base.archivesName = "LittleMandarinClassics"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.littlemandarin.classics"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    buildFeatures {
        compose = true
    }

    signingConfigs {
        create("release") {
            if (releaseSigningRequested) {
                storeFile = file(requiredReleaseSigningEnv("ANDROID_RELEASE_KEYSTORE_PATH"))
                storePassword = requiredReleaseSigningEnv("ANDROID_RELEASE_STORE_PASSWORD")
                keyAlias = requiredReleaseSigningEnv("ANDROID_RELEASE_KEY_ALIAS")
                keyPassword = requiredReleaseSigningEnv("ANDROID_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))

    debugImplementation(libs.androidx.compose.ui.tooling)
}
