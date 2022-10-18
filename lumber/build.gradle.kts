plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    testImplementation(libs.kotest.runner.junit5)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        if (System.getenv("CI") == "true") {
            events("PASSED", "FAILED", "SKIPPED")
        }
        setExceptionFormat("full")
    }
}