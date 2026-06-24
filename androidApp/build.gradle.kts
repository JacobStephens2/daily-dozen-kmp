import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

// Release signing is configured from the environment first (CI: secrets exported
// as env vars) and falls back to a gitignored keystore.properties for local
// release builds. When neither is present the release build is simply left
// unsigned, so debug builds and fresh clones still work with no secrets.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) FileInputStream(file).use { load(it) }
}
fun signingValue(envKey: String, propKey: String): String? =
    System.getenv(envKey) ?: keystoreProperties.getProperty(propKey)

android {
    namespace = "page.stephens.dailydozen.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "page.stephens.dailydozen"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 7
        versionName = "0.3.2"
    }

    signingConfigs {
        create("release") {
            val storeFilePath = signingValue("KEYSTORE_FILE", "storeFile")
            if (storeFilePath != null) {
                // rootProject.file() handles both absolute paths (CI) and paths
                // relative to the repo root (local keystore.properties).
                storeFile = rootProject.file(storeFilePath)
                storePassword = signingValue("KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            // Only attach the release signing config when a keystore was supplied.
            signingConfigs.getByName("release").storeFile?.let {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    // Compose Android artifacts, versions managed by the Compose MP plugin.
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(libs.koin.android)
}
