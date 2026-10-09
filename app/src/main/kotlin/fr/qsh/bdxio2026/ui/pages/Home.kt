package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.AUTH
import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.ProjectService
import io.ktor.server.auth.*
import io.ktor.server.routing.*
import kotlinx.html.*

const val ROUTE_HOME = "/"

fun Routing.homePage(
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
                    +"Hello World!"
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
