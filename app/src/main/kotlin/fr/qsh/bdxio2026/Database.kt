package fr.qsh.bdxio2026

import com.mongodb.kotlin.client.coroutine.MongoClient
import opensavvy.ktmongo.coroutines.CoroutineMongoDatabase
import opensavvy.ktmongo.coroutines.asKtMongo

fun configureDatabase(): CoroutineMongoDatabase {
    val client = MongoClient.create("mongodb://localhost:27030")
        .asKtMongo()

    return client.database("bdxio2026")
}
