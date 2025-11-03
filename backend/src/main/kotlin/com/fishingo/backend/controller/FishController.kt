package com.fishingo.backend.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.fishingo.backend.service.FishService
import com.fishingo.backend.model.Fish

fun Route.fishRoutes(fishService: FishService) {

    route("/fish") {

        get {
            call.respond(fishService.getAllFish())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(fishService.getFishById(id) ?: "Fish not found")
        }

        post {
            val fish = call.receive<Fish>()
            val created = fishService.createFish(fish)
            call.respond(created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(mapOf("deleted" to fishService.deleteFish(id)))
        }
    }
}
