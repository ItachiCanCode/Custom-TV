plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
base { archivesName.set("VoidLauncher") }

android {
    namespace = "com.example.voidlauncher"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.voidlauncher"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint {
        checkOnly += listOf("NewApi", "InlinedApi")
        abortOnError = true
    }
}
