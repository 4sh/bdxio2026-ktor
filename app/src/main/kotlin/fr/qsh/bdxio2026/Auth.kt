package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.UserService
import fr.qsh.bdxio2026.ui.pages.ROUTE_LOGIN
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ObjectId

@Serializable
data class AuthCookiePayload(
    val userId: ObjectId,
    val token: String,
)

const val AUTH = "auth-session"

fun Application.configureAuth(
    users: UserService,
) {
    install(Sessions) {
        cookie<AuthCookiePayload>("token") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = UserService.tokenExpirationThreshold.inWholeSeconds
            cookie.httpOnly = !this@configureAuth.developmentMode
            cookie.secure = !this@configureAuth.developmentMode
        }
    }

    install(Authentication) {
        session<AuthCookiePayload>(AUTH) {
            validate { session ->
                try {
                    session
                        .takeIf { users.verifyToken(session.userId, session.token) }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        ensureActive()
                    }
                    null
                }
            }

            challenge {
                call.respondRedirect(ROUTE_LOGIN)
            }
        }
    }
}
