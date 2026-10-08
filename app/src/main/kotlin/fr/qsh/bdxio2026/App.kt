package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.PingService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8080,
        module = Application::app,
    ).start(wait = true)
}

suspend fun Application.app() {
    configureHttp()

    val db = configureDatabase()
    val ping = PingService(db.collection("pings"))

    configureRoutes(ping)
}
