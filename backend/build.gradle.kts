plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

dependencies {
    implementation("org.mindrot:jbcrypt:0.4")

    implementation(platform("io.ktor:ktor-bom:2.3.6"))

    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-content-negotiation-jvm")
    implementation("io.ktor:ktor-server-status-pages-jvm")

    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm")

    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.50.1")

    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    // This MUST match the package where your fun main() is
    mainClass.set("com.fishingo.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}

// ❌ NO ShadowJar block here anymore
