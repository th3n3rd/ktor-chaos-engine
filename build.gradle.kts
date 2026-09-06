plugins {
    kotlin("jvm") version "2.4.10"
}

group = "io.github.th3n3rd"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:3.1.1")
    implementation("io.ktor:ktor-client-mock:3.1.1")
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core:6.2.4")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}