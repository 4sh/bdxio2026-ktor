package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.AUTH
import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.ProjectService
import io.ktor.http.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*
import opensavvy.ktmongo.bson.types.ObjectId

const val ROUTE_PROJECT_NEW = "/projects/new"

fun routeProject(id: ObjectId) = "/projects/${id.hex}"

fun Routing.projectPages(
    projects: ProjectService,
) {
    suspend fun RoutingContext.respondNewProjectPage(
        name: String?,
        blankName: Boolean = false,
    ) {
        call.respondPage(
            title = "New project",
            status = when {
                blankName -> HttpStatusCode.UnprocessableEntity
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

                    if (blankName)
                        +" The name cannot be blank"
                }

                input(InputType.submit)
            }
        }
    }

    authenticate(AUTH) {
        get(ROUTE_PROJECT_NEW) {
            respondNewProjectPage(name = null)
        }

        post(ROUTE_PROJECT_NEW) {
            val user = call.principal<AuthCookiePayload>()!!
            val name = call.receiveParameters()["name"]!!.trim()

            if (name.isEmpty()) {
                respondNewProjectPage(name = name, blankName = true)
                return@post
            }

            val id = projects.createProject(name, user.userId)

            call.respondRedirect(routeProject(id))
        }

        get("/projects/{id}") {
            val user = call.principal<AuthCookiePayload>()!!

            val id = try {
                ObjectId(call.parameters["id"]!!)
            } catch (_: IllegalArgumentException) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            val project = projects.getById(id, user.userId)

            if (project == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }

            call.respondPage(
                title = project.name,
            ) {
                h1 {
                    +project.name
                }
            }
        }
    }
}
