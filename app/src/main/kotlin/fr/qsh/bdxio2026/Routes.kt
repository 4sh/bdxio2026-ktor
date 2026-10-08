package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.UserService
import fr.qsh.bdxio2026.ui.pages.createUser
import fr.qsh.bdxio2026.ui.pages.homePage
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*

fun Application.configureRoutes(
    users: UserService,
) {
    routing {
        homePage()
        createUser(users)

        staticResources("/assets", "assets")
    }
}
