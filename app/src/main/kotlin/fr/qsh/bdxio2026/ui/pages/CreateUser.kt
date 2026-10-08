package fr.qsh.bdxio2026.ui.pages

import fr.qsh.bdxio2026.services.UserService
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.*

const val ROUTE_CREATE_USER = "/create-user"

fun Routing.createUser(
    users: UserService,
) {
    suspend fun RoutingContext.respondAccountPage(
        username: String?,
        password: String?,
        passwordConfirmation: String?,
        passwordMismatch: Boolean = false,
        usernameTaken: Boolean = false,
    ) {
        call.respondPage(
            title = "Create account",
            status = when {
                passwordMismatch || usernameTaken -> HttpStatusCode.UnprocessableEntity
                else -> HttpStatusCode.OK
            }
        ) {
            h1 {
                +"Create account"
            }

            form(
                action = ROUTE_CREATE_USER,
                method = FormMethod.post,
                encType = FormEncType.applicationXWwwFormUrlEncoded
            ) {
                p {
                    label {
                        +"Username: "
                        input(InputType.text, name = "username") {
                            minLength = "3"
                            required = true
                            autoFocus = true
                            autoComplete = "email"

                            if (username != null)
                                value = username
                        }
                    }

                    if (usernameTaken)
                        +" Username already taken"
                }

                p {
                    label {
                        +"Password: "
                        input(InputType.password, name = "password") {
                            minLength = "6"
                            required = true
                            autoComplete = "new-password"

                            if (password != null)
                                value = password
                        }
                    }
                }

                p {
                    label {
                        +"Confirm password: "
                        input(InputType.password, name = "password-confirm") {
                            minLength = "6"
                            required = true
                            autoComplete = "new-password"

                            if (passwordConfirmation != null)
                                value = passwordConfirmation
                        }
                    }

                    if (passwordMismatch)
                        +" Passwords do not match"
                }

                input(InputType.submit)
            }
        }
    }


    get(ROUTE_CREATE_USER) {
        respondAccountPage(
            username = null,
            password = null,
            passwordConfirmation = null,
        )
    }

    post(ROUTE_CREATE_USER) {
        val params = call.receiveParameters()
        val username = params["username"]!!
        val password = params["password"]!!
        val passwordConfirmation = params["password-confirm"]!!

        if (password != passwordConfirmation) {
            respondAccountPage(
                username = username,
                password = password,
                passwordConfirmation = passwordConfirmation,
                passwordMismatch = true,
            )
            return@post
        }

        if (!users.isUsernameAvailable(username)) {
            respondAccountPage(
                username = username,
                password = password,
                passwordConfirmation = passwordConfirmation,
                usernameTaken = true,
            )
            return@post
        }

        users.createAccount(username, password)

        call.respondRedirect(ROUTE_HOME)
    }
}
