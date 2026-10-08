package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.services.PingService
import io.ktor.server.routing.*
import kotlinx.html.p

fun Routing.homePage(
    pings: PingService,
) {
    get("/") {
        pings.ping("/")

        val count = pings.countPings()

        call.respondPage(
            title = "Home",
        ) {
            p {
                +"Hello World! You're the visitor #$count"
            }
        }
    }
}
