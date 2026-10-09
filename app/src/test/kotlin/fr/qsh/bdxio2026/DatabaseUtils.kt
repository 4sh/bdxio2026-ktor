package fr.qsh.bdxio2026

import com.mongodb.kotlin.client.coroutine.MongoClient
import opensavvy.ktmongo.coroutines.asKtMongo
import opensavvy.prepared.suite.prepared

val db by prepared {
    MongoClient.create("mongodb://localhost:27030")
        .asKtMongo()
        .database("bdxio2026-test")
}
