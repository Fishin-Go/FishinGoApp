package com.fishingo.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*

import com.fishingo.backend.service.*
import com.fishingo.backend.repository.*
import com.fishingo.backend.controller.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Enable JSON
    install(ContentNegotiation) {
        json()
    }

    // Initialize services
    val userService = UserService(UserRepository())
    val fishService = FishService(FishRepository())
    val catchService = CatchService(CatchRepository())
    val waterBodyService = WaterBodyService(WaterBodyRepository())

    // Register routes
    routing {
        userRoutes(userService)
        fishRoutes(fishService)
        catchRoutes(catchService)
        waterBodyRoutes(waterBodyService)
    }
}
