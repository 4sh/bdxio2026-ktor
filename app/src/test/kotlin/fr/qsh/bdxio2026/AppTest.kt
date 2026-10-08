package fr.qsh.bdxio2026

import io.ktor.client.request.*
import io.ktor.client.statement.*
import opensavvy.prepared.compat.ktor.preparedClient
import opensavvy.prepared.compat.ktor.preparedServer
import opensavvy.prepared.runner.testballoon.preparedSuite

val AppTest by preparedSuite {

    val server by preparedServer {
        application {
            app()
        }
    }

    val client by server.preparedClient()

    test("Hello world") {
        check(client().get("/").bodyAsText() == "Hello World!")
    }
}
