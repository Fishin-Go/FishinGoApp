package com.fishingo.backend
import com.fishingo.backend.controller.catchRoutes
import com.fishingo.backend.controller.userRoutes
import com.fishingo.backend.database.DatabaseFactory
import com.fishingo.backend.repository.UserRepository
import com.fishingo.backend.service.UserService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.uri
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(
        Netty,
        port = port,
        host = "0.0.0.0"
    ) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    // Logger for error reporting
    val log = LoggerFactory.getLogger("FishinGo")

    // 1. Connect to Aiven Postgres (via Exposed / Hikari)
    DatabaseFactory.init()

    // 2. JSON (kotlinx.serialization) for request/response bodies
    install(ContentNegotiation) {
        json()
    }

    // 3. Log all unhandled exceptions and return 500 instead of crashing
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            log.error("Unhandled error on ${call.request.uri}", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                "Internal server error"
            )
        }
    }

    // 4. Wire repositories + services
    val userService = UserService(UserRepository())

    // 5. Routes (HTTP endpoints)
    routing {
        // Simple test endpoint
        get("/") {
            call.respondText("FishinGo backend is running ✅")
        }

        userRoutes(userService)
        catchRoutes()
    }
}
