plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
base { archivesName.set("VoidLauncherSetup") }

android {
    namespace = "io.github.itachicancode.voidlauncher.setup"
    compileSdk = 34
    defaultConfig {
        applicationId = "io.github.itachicancode.voidlauncher.setup"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources { excludes += setOf("META-INF/*.kotlin_module", "META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties") } }
}
dependencies {
    implementation("dev.mobile:dadb:1.2.10")
    testImplementation("junit:junit:4.13.2")
}
