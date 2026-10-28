package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.services.UserService
import fr.qsh.bdxio2026.ui.pages.auth.createUser
import fr.qsh.bdxio2026.ui.pages.auth.logIn
import fr.qsh.bdxio2026.ui.pages.home.homePage
import fr.qsh.bdxio2026.ui.pages.project.projectPages
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.routing.*
import kotlin.time.Clock

fun Application.configureRoutes(
    users: UserService,
    projects: ProjectService,
    clock: Clock,
) {
    routing {
        homePage(projects)
        createUser(users)
        projectPages(projects, users)
        logIn(users)

        staticResources("/assets", "assets")
    }
}
