package fr.qsh.bdxio2026

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.compression.*
import io.ktor.server.plugins.conditionalheaders.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*

fun Application.configureHttp() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(Compression)

    install(ConditionalHeaders)

    install(CallLogging)

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.respondText(
                text = if (this@configureHttp.developmentMode) "500: $cause" else "Server error",
                status = HttpStatusCode.InternalServerError,
            )

            cause.printStackTrace()
        }
    }

    install(ContentNegotiation) {
        json()
    }
}
