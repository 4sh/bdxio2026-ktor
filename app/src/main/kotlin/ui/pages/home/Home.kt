package fr.qsh.bdxio2026.ui.pages.home

import fr.qsh.bdxio2026.AUTH
import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.ui.pages.auth.ROUTE_CREATE_USER
import fr.qsh.bdxio2026.ui.pages.auth.ROUTE_LOGIN
import fr.qsh.bdxio2026.ui.pages.auth.ROUTE_LOGOUT
import fr.qsh.bdxio2026.ui.pages.project.ROUTE_PROJECT_NEW
import fr.qsh.bdxio2026.ui.pages.project.routeProject
import fr.qsh.bdxio2026.ui.pages.respondPage
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.html.*

const val ROUTE_HOME = "/"

fun Route.homePage(
    projects: ProjectService,
) {
    authenticate(AUTH, optional = true) {
        get(ROUTE_HOME) {
            val user = call.principal<AuthCookiePayload>()
            val myProjects = user?.let { projects.getMine(it.userId) }.orEmpty()

            call.respondPage(
                title = "Home",
            ) {
                p {
                    +"Welcome!"
                }

                p {
                    +"This application allows multiple users to share expenses during shared trips."
                }

                if (user != null) {
                    h2 {
                        +"Projects"
                    }

                    if (myProjects.isEmpty()) {
                        p {
                            +"No projects yet."
                        }
                    } else {
                        ul {
                            for (project in myProjects) {
                                li {
                                    a(href = routeProject(project._id)) {
                                        +project.name
                                    }
                                }
                            }
                        }
                    }

                    a(href = ROUTE_PROJECT_NEW) {
                        +"New project"
                    }

                    +" • "

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
