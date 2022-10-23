plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "lumber.log.android.sample"
    compileSdk = 33

    defaultConfig {
        applicationId = "lumber.log.android.sample"
        minSdk = 16
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(project(":lumber-sample"))
    implementation(project(":lumber-android"))
    implementation(libs.androidx.startup)
}
