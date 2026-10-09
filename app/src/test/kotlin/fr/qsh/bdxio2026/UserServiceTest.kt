@file:OptIn(ExperimentalCoroutinesApi::class)

package fr.qsh.bdxio2026

import fr.qsh.bdxio2026.services.UserService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import opensavvy.prepared.runner.testballoon.preparedSuite
import opensavvy.prepared.suite.clock
import opensavvy.prepared.suite.prepared
import opensavvy.prepared.suite.time
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlin.time.Duration.Companion.seconds

val username by prepared {
    "test-user-${Random.nextUInt()}"
}

val users by prepared {
    UserService(
        collection = db().collection("users"),
        clock = time.clock,
    )
}

val UserServiceTest by preparedSuite {

    test("Create user") {
        users().createAccount(username(), "whatever")

        check(users().verifyCredentials(username(), "whatever") != null)
    }

    test("Create tokens") {
        val userId = users().createAccount(username(), "whatever")

        val token = users().createToken(username())

        check(users().verifyToken(userId, token))
    }

    test("Tokens are expired after ${UserService.tokenExpirationThreshold}") {
        coroutineScope {
            val userId = users().createAccount(username(), "whatever")

            val token1 = users().createToken(username())

            val token2 = async {
                delay(UserService.tokenExpirationThreshold / 2)
                users().createToken(username())
            }

            delay(UserService.tokenExpirationThreshold + 1.seconds)

            val token3 = users().createToken(username())

            check(!users().verifyToken(userId, token1))
            check(users().verifyToken(userId, token2.await()))
            check(users().verifyToken(userId, token3))
        }
    }

    test("A token that has been used does not expire") {
        coroutineScope {
            val userId = users().createAccount(username(), "whatever")

            val tokenUnused = users().createToken(username())
            val tokenUsed = users().createToken(username())

            delay(UserService.tokenExpirationThreshold / 2)
            check(users().verifyToken(userId, tokenUsed))

            delay(UserService.tokenExpirationThreshold / 2 + 1.seconds)

            check(!users().verifyToken(userId, tokenUnused))
            check(users().verifyToken(userId, tokenUsed))
        }
    }

}
