package com.fishingo.backend.controller

import com.fishingo.backend.dto.LoginRequest
import com.fishingo.backend.dto.RegisterRequest
import com.fishingo.backend.dto.UserResponse
import com.fishingo.backend.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.userRoutes(
    userService: UserService = UserService()
) {
    // GET /users  (list all users)
    get("/users") {
        val users = userService.getAllUsers()

        val response: List<UserResponse> = users.map {
            UserResponse(
                id = it.id!!,
                username = it.username,
                email = it.email
            )
        }

        call.respond(response)
    }

    // GET /users/{id}
    get("/users/{id}") {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respondText("Invalid id", status = HttpStatusCode.BadRequest)
            return@get
        }

        val user = userService.getUser(id)
        if (user == null) {
            call.respondText("User not found", status = HttpStatusCode.NotFound)
        } else {
            call.respond(
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email
                )
            )
        }
    }

    // POST /register
    post("/register") {
        val body = call.receive<RegisterRequest>()

        val user = userService.register(
            username = body.username,
            email = body.email,
            password = body.password
        )

        if (user == null) {
            // Email already used
            call.respondText(
                text = "Email is already registered",
                status = HttpStatusCode.Conflict
            )
        } else {
            call.respond(
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email
                )
            )
        }
    }

    // POST /login
    post("/login") {
        val body = call.receive<LoginRequest>()

        val user = userService.login(
            email = body.email,
            password = body.password
        )

        if (user == null) {
            call.respondText("Invalid credentials", status = HttpStatusCode.Unauthorized)
        } else {
            call.respond(
                UserResponse(
                    id = user.id!!,
                    username = user.username,
                    email = user.email
                )
            )
        }
    }
}
