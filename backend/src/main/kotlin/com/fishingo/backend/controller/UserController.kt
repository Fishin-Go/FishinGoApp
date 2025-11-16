package com.fishingo.backend.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.fishingo.backend.service.UserService
import com.fishingo.backend.model.User

fun Route.userRoutes(userService: UserService) {

    route("/users") {

        get {
            call.respond(userService.getAllUsers())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(userService.getUserById(id) ?: "User not found")
        }

        post {
            val user = call.receive<User>()
            val created = userService.createUser(user)
            call.respond(created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(mapOf("deleted" to userService.deleteUser(id)))
        }
    }
}
