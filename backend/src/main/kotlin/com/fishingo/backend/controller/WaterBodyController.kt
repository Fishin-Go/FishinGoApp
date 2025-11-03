package com.fishingo.backend.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.fishingo.backend.service.WaterBodyService
import com.fishingo.backend.model.WaterBody

fun Route.waterBodyRoutes(waterBodyService: WaterBodyService) {

    route("/waterbodies") {

        get {
            call.respond(waterBodyService.getAllWaterBodies())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(waterBodyService.getWaterBodyById(id) ?: "Water body not found")
        }

        post {
            val waterBody = call.receive<WaterBody>()
            val created = waterBodyService.createWaterBody(waterBody)
            call.respond(created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(mapOf("deleted" to waterBodyService.deleteWaterBody(id)))
        }
    }
}
