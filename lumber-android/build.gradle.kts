plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.mavenPublish)
}

android {
    namespace = "lumber.log.android"
    compileSdk = 33
    defaultConfig {
        minSdk = 14
        aarMetadata {
            minCompileSdk = 14
        }
    }

    publishing {
        multipleVariants {
            allVariants()
            withJavadocJar()
        }
    }
}

dependencies {
    api(project(":lumber"))

    testImplementation(libs.google.truth)
    testImplementation(libs.junit.junit)
    testImplementation(libs.robolectric)
}