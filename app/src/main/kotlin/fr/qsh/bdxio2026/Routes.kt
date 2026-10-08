package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.PingService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRoutes(pings: PingService) {
    routing {

        get("/") {
            pings.ping("/")

            call.respondText("Hello World! You're the visitor #${pings.countPings()}")
        }
    }
}
