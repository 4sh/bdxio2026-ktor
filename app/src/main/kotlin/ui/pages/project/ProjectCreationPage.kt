package fr.qsh.bdxio2026.ui.pages.project

import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.ui.pages.respondPage
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

const val ROUTE_PROJECT_NEW = "/projects/new"

fun Route.createProject(
    projects: ProjectService,
) {
    suspend fun RoutingContext.respondNewProjectPage(
        name: String?,
        nameWasBlank: Boolean = false,
    ) {
        call.respondPage(
            title = "New project",
            status = when {
                nameWasBlank -> HttpStatusCode.UnprocessableEntity
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +"New project"
            }

            form(
                action = ROUTE_PROJECT_NEW,
                method = FormMethod.post,
                encType = FormEncType.applicationXWwwFormUrlEncoded
            ) {
                p {
                    label {
                        +"Name: "
                        input(InputType.text, name = "name") {
                            required = true
                            autoFocus = true

                            if (name != null)
                                value = name
                        }
                    }

                    if (nameWasBlank)
                        +" The name cannot be blank"
                }

                input(InputType.submit)
            }
        }
    }

    get(ROUTE_PROJECT_NEW) {
        respondNewProjectPage(name = null)
    }

    post(ROUTE_PROJECT_NEW) {
        val user = call.principal<AuthCookiePayload>()!!
        val name = call.receiveParameters()["name"]!!.trim()

        if (name.isEmpty()) {
            respondNewProjectPage(name = name, nameWasBlank = true)
            return@post
        }

        val id = projects.createProject(name, user.userId)

        call.respondRedirect(routeProject(id))
    }
}
