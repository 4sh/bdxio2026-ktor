package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ByteVector
import opensavvy.ktmongo.bson.types.InstantAsBsonDatetimeSerializer
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.Uuid

@Serializable
data class User(
    @Suppress("PropertyName")
    val _id: ObjectId,

    val username: String,

    val passwordHash: ByteVector,

    val passwordSalt: ByteVector,

    val passwordIterations: Int,

    val tokens: List<UserTokens> = emptyList(),
)

@OptIn(ExperimentalTime::class)
@Serializable
data class UserTokens(
    val tokenHash: ByteVector,
    val tokenSalt: ByteVector,
    val tokenIterations: Int,
    val lastUsed: @Serializable(InstantAsBsonDatetimeSerializer::class) Instant,
)

class UserService(
    private val collection: CoroutineMongoCollection<User>,
    private val clock: Clock,
) {
    private val random = SecureRandom()
    private val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")

    suspend fun isUsernameAvailable(username: String): Boolean =
        !collection.exists { User::username eq username }

    suspend fun createAccount(
        username: String,
        password: String,
    ) {
        val id = collection.newId()

        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iterations = 65536
        val hash = hashPassword(password, salt, iterations)

        collection.insertOne(
            User(
                _id = id,
                username = username,
                passwordHash = ByteVector(bytes = hash),
                passwordSalt = ByteVector(bytes = salt),
                passwordIterations = iterations,
            )
        )
    }

    suspend fun verifyCredentials(
        username: String,
        password: String,
    ): Boolean {
        val user = collection.findOne { User::username eq username } ?: return false

        val hash = hashPassword(password, user.passwordSalt.toArray(), user.passwordIterations)

        return MessageDigest.isEqual(hash, user.passwordHash.toArray())
    }

    suspend fun createToken(
        username: String,
    ): String {
        val salt = ByteArray(16)
        random.nextBytes(salt)

        val iterations = 16384
        val token = Uuid.random().toHexDashString()
        val hash = hashPassword(token, salt, iterations)

        collection.filter { User::username eq username }
            .updateOne {
                User::tokens push UserTokens(
                    tokenHash = ByteVector(bytes = hash),
                    tokenSalt = ByteVector(bytes = salt),
                    tokenIterations = iterations,
                    lastUsed = clock.now(),
                )
            }

        return token
    }

    suspend fun verifyToken(
        username: String,
        token: String,
    ): Boolean {
        val user = collection.findOne {
            User::username eq username
            User::tokens.exists()
        } ?: return false

        val now = clock.now()
        val expirationDate = now - tokenExpirationThreshold

        val (validTokens, expiredTokens) = user.tokens.partition { it.lastUsed >= expirationDate }

        val validToken = validTokens.firstOrNull {
            MessageDigest.isEqual(
                it.tokenHash.toArray(),
                hashPassword(token, it.tokenSalt.toArray(), it.tokenIterations),
            )
        }
        val isValid = validToken != null

        collection.filter { User::_id eq user._id }
            .updateOne {
                if (expiredTokens.isNotEmpty()) {
                    User::tokens pull {
                        UserTokens::lastUsed lt expirationDate
                    }
                }

                if (isValid) {
                    User::tokens.filter {
                        it / UserTokens::tokenHash eq validToken.tokenHash
                    } / UserTokens::lastUsed set now
                }
            }

        return isValid
    }

    private fun hashPassword(password: String, salt: ByteArray, iterations: Int): ByteArray =
        keyFactory.generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, 128)).encoded

    companion object {
        val tokenExpirationThreshold = 7.days
    }
}
