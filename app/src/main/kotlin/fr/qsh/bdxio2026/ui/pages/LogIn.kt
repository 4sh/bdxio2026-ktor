package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.services.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.date.*
import kotlinx.html.*
import kotlin.time.Clock

const val ROUTE_LOGIN = "/login"

fun Routing.logIn(
    users: UserService,
    clock: Clock,
) {
    suspend fun RoutingContext.respondLoginPage(
        username: String?,
        invalidCredentials: Boolean = false,
    ) {
        call.respondPage(
            title = "Log in",
            status = when {
                invalidCredentials -> HttpStatusCode.Unauthorized
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +"Log in"
            }

            form(
                action = ROUTE_LOGIN,
                method = FormMethod.post,
                encType = FormEncType.applicationXWwwFormUrlEncoded
            ) {
                p {
                    label {
                        +"Username: "
                        input(InputType.text, name = "username") {
                            required = true
                            autoFocus = true
                            autoComplete = "username"

                            if (username != null)
                                value = username
                        }
                    }
                }

                p {
                    label {
                        +"Password: "
                        input(InputType.password, name = "password") {
                            required = true
                            autoComplete = "current-password"
                        }
                    }

                    if (invalidCredentials)
                        +" Invalid username or password"
                }

                input(InputType.submit)
            }
        }
    }

    get(ROUTE_LOGIN) {
        respondLoginPage(username = null)
    }

    post(ROUTE_LOGIN) {
        val params = call.receiveParameters()
        val username = params["username"]!!
        val password = params["password"]!!

        if (!users.verifyCredentials(username, password)) {
            respondLoginPage(
                username = username,
                invalidCredentials = true,
            )
            return@post
        }

        val token = users.createToken(username)

        call.response.cookies.append(
            name = "token",
            value = token,
            expires = GMTDate((clock.now() + UserService.tokenExpirationThreshold).toEpochMilliseconds()),
            secure = !application.developmentMode,
            httpOnly = !application.developmentMode,
        )
        call.respondRedirect(ROUTE_HOME)
    }
}
