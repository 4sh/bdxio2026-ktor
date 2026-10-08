package org.example

import opensavvy.prepared.runner.testballoon.preparedSuite

val AppTest by preparedSuite {
    test("app has a greeting") {
        val classUnderTest = App()
        check(classUnderTest.greeting == "Hello World!")
    }
}
