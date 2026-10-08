package fr.qsh.bdxio2026

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRoutes() {
    routing {

        get("/") {
            call.respondText("Hello World!")
        }
    }
}
