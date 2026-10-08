package fr.qsh.bdxio2026

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
    configureRoutes()
}
