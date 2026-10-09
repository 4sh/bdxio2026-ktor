package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.AuthCookiePayload
import fr.qsh.bdxio2026.services.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.html.*
import kotlin.time.Clock

const val ROUTE_LOGIN = "/login"
const val ROUTE_LOGOUT = "/logout"

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

        val userId = users.verifyCredentials(username, password)

        if (userId == null) {
            respondLoginPage(
                username = username,
                invalidCredentials = true,
            )
            return@post
        }

        val token = users.createToken(username)

        call.sessions.set(AuthCookiePayload(userId, token))
        call.respondRedirect(ROUTE_HOME)
    }

    get(ROUTE_LOGOUT) {
        val cookie = call.sessions.get<AuthCookiePayload>()

        call.sessions.clear<AuthCookiePayload>()
        call.respondRedirect(ROUTE_HOME)

        if (cookie != null)
            users.deleteToken(cookie.userId, cookie.token)
    }
}
