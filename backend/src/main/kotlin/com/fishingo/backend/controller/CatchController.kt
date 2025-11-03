package com.fishingo.backend.controller

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.fishingo.backend.service.CatchService
import com.fishingo.backend.model.Catch

fun Route.catchRoutes(catchService: CatchService) {

    route("/catches") {

        get {
            call.respond(catchService.getAllCatches())
        }

        get("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(catchService.getCatchById(id) ?: "Catch not found")
        }

        post {
            val newCatch = call.receive<Catch>()
            val created = catchService.createCatch(newCatch)
            call.respond(created)
        }

        delete("/{id}") {
            val id = call.parameters["id"]?.toIntOrNull()
            if (id == null) call.respondText("Invalid ID", status = io.ktor.http.HttpStatusCode.BadRequest)
            else call.respond(mapOf("deleted" to catchService.deleteCatch(id)))
        }
    }
}
