package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.AUTH
import fr.qsh.bdxio2026.AuthCookiePayload
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.html.a
import kotlinx.html.p

const val ROUTE_HOME = "/"

fun Routing.homePage() {
    authenticate(AUTH, optional = true) {
        get(ROUTE_HOME) {
            val isAuthenticated = call.principal<AuthCookiePayload>()

            call.respondPage(
                title = "Home",
            ) {
                p {
                    +"Hello World!"
                }

                if (isAuthenticated != null) {
                    a(href = ROUTE_LOGOUT) {
                        +"Log out"
                    }
                } else {
                    a(href = ROUTE_CREATE_USER) {
                        +"Create account"
                    }

                    +" • "

                    a(href = ROUTE_LOGIN) {
                        +"Log in"
                    }
                }
            }
        }
    }
}
