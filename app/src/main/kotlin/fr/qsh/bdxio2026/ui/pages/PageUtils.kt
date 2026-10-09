package fr.qsh.bdxio2026.ui.pages

import io.ktor.http.*
import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

suspend fun RoutingCall.respondPage(
    title: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    content: DIV.() -> Unit,
) {
    respondHtml(status = status) {
        head {
            meta(charset = "utf-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1")
            meta(name = "author", content = "4SH")
            link("/assets/style.css", rel = "stylesheet")
            title("$title • BDX I/O 2026 Demo")
        }

        body {
            nav {
                a("/") {
                    +"Home"
                }
            }

            div {
                id = "content"

                content()
            }
        }
    }
}
