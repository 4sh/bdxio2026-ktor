package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.bson.types.ByteVector
import opensavvy.ktmongo.bson.types.ObjectId
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection
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

        val key = PBEKeySpec(password.toCharArray(), salt, 65536, 128)
        val output = keyFactory.generateSecret(key)

        collection.insertOne(
            User(
                _id = id,
                username = username,
                passwordHash = ByteVector(bytes = output.encoded),
                passwordSalt = ByteVector(bytes = salt),
                passwordIterations = key.iterationCount,
            )
        )
    }
}
