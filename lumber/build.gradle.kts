plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
   testImplementation(libs.bundles.testing)
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        if (System.getenv("CI") == "true") {
            events("failed", "skipped", "passed")
        }
        setExceptionFormat("full")
    }
}