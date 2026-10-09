package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.ProjectService
import fr.qsh.bdxio2026.services.UserService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlin.time.Clock

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = Application::app,
    ).start(wait = true)
}

suspend fun Application.app() {
    val clock = Clock.System

    configureHttp()

    val otel = getOpenTelemetry("app")
    configureOpenTelemetry(otel)

    val db = configureDatabase(otel)
    val users = UserService(db.collection("users"), clock)
    val projects = ProjectService(db.collection("projects"), db.collection("transactions"))

    configureAuth(users)
    configureRoutes(users, projects, clock)
}
