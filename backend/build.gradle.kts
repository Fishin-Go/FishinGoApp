plugins {
    kotlin("jvm")
    id("application")
}

dependencies {
    // --- Ktor server ---
    implementation("io.ktor:ktor-server-core:2.3.6")
    implementation("io.ktor:ktor-server-netty:2.3.6")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.6")
    implementation("io.ktor:ktor-serialization-gson:2.3.6")

    // Ktor core & server engine
    implementation("io.ktor:ktor-server-core-jvm:2.3.6")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.6")

    // Routing + Content negotiation + JSON
    implementation("io.ktor:ktor-server-routing-jvm:2.3.6")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.6")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.6")

    // --- Exposed ORM ---
    implementation("org.jetbrains.exposed:exposed-core:0.50.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.50.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.50.1")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:0.50.1")
    implementation("org.jetbrains.exposed:exposed-java-time:0.47.0")

    // --- PostgreSQL driver ---
    implementation("org.postgresql:postgresql:42.7.2")

    // --- Logging ---
    implementation("ch.qos.logback:logback-classic:1.4.14")
}

application {
    mainClass.set("com.fishingo.backend.ApplicationKt")
}

kotlin {
    jvmToolchain(21)
}
