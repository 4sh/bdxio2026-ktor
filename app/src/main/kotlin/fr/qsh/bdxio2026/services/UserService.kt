package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ByteVector
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Serializable
data class User(
    @Suppress("PropertyName")
    val _id: ObjectId,

    val username: String,

    val passwordHash: ByteVector,

    val passwordSalt: ByteVector,

    val passwordIterations: Int,
)

class UserService(
    private val collection: CoroutineMongoCollection<User>,
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
        val user = collection.find { User::username eq username }.firstOrNull() ?: return false

        val hash = hashPassword(password, user.passwordSalt.toArray(), user.passwordIterations)

        return MessageDigest.isEqual(hash, user.passwordHash.toArray())
    }

    private fun hashPassword(password: String, salt: ByteArray, iterations: Int): ByteArray =
        keyFactory.generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, 128)).encoded
}
