package fr.qsh.bdxio2026.services

import kotlinx.serialization.Serializable
import opensavvy.ktmongo.coroutines.CoroutineMongoCollection

@Serializable
data class Ping(
    val name: String,
)

class PingService(
    private val collection: CoroutineMongoCollection<Ping>,
) {

    suspend fun ping(name: String) {
        collection.insertOne(Ping(name))
    }

    suspend fun countPings() =
        collection.count()
}
