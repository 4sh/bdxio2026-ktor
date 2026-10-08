package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.PingService
import fr.qsh.bdxio2026.ui.pages.homePage
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRoutes(pings: PingService) {
    routing {
        homePage(pings)

        staticResources("/assets", "assets")
    }
}
