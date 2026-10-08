package fr.qsh.bdxio2026.ui.pages

import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.p

const val ROUTE_HOME = "/"

fun Routing.homePage() {
    get(ROUTE_HOME) {
        call.respondPage(
            title = "Home",
        ) {
            p {
                +"Hello World!"
            }

            a(href = ROUTE_CREATE_USER) {
                +"Create account"
            }
        }
    }
}
