package com.fishingo.backend.controller

import com.fishingo.backend.dto.NewCatchRequest
import com.fishingo.backend.service.CatchService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.catchRoutes(
    catchService: CatchService = CatchService()
) {
    route("/catches") {

        // POST /catches?userId=123
        post {
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing or invalid userId")
                return@post
            }

            val body = call.receive<NewCatchRequest>()
            val created = catchService.createCatch(userId, body)

            call.respond(HttpStatusCode.Created, created)
        }

        // GET /catches?userId=123
        get {
            val userId = call.request.queryParameters["userId"]?.toIntOrNull()
            if (userId == null) {
                call.respond(HttpStatusCode.BadRequest, "Missing or invalid userId")
                return@get
            }

            val catches = catchService.getCatchesForUser(userId)
            call.respond(catches)
        }
    }
}
