import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.palantir.git.version)
    alias(libs.plugins.vanniktech.maven.publish)
}

val gitVersion: groovy.lang.Closure<String> by extra

group = "io.github.th3n3rd"
version = gitVersion()

repositories {
    mavenCentral()
}

dependencies {
    api(libs.ktor.client.core)
    api(libs.ktor.client.mock)
    api(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.cio)
    testImplementation(libs.ktor.client.okhttp)
    testImplementation(libs.ktor.client.apache)
    testImplementation(libs.ktor.client.java)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.cio)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.kotest.assertions.json)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.slf4j.nop)
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED,
            TestLogEvent.FAILED,
        )
    }
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()

    pom {
        name = "Ktor Chaos Engine"
        description = "Chaos testing engine for Ktor HTTP clients."
        url = "https://github.com/th3n3rd/ktor-chaos-engine"

        licenses {
            license {
                name = "Apache-2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }

        developers {
            developer {
                id = "th3n3rd"
                name = "Marco Garofalo"
            }
        }

        scm {
            url = "https://github.com/th3n3rd/ktor-chaos-engine"
            connection = "scm:git:https://github.com/th3n3rd/ktor-chaos-engine.git"
        }
    }
}