package fr.qsh.bdxio2026

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.observability.ObservabilitySettings
import io.micrometer.observation.ObservationRegistry
import io.micrometer.tracing.handler.DefaultTracingObservationHandler
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext
import io.micrometer.tracing.otel.bridge.OtelTracer
import io.opentelemetry.api.OpenTelemetry
import opensavvy.ktmongo.coroutines.CoroutineMongoDatabase
import opensavvy.ktmongo.coroutines.asKtMongo

fun configureDatabase(otel: OpenTelemetry): CoroutineMongoDatabase {
    val client = MongoClient.create(
        MongoClientSettings.builder()
            .applyConnectionString(ConnectionString("mongodb://localhost:27030"))
            .observabilitySettings(
                ObservabilitySettings.micrometerBuilder()
                    .observationRegistry(observationRegistry(otel))
                    .build()
            )
            .build()
    )
        .asKtMongo()

    return client.database("bdxio2026")
}

/**
 * Bridges the MongoDB driver's native Micrometer observations to OpenTelemetry spans.
 */
private fun observationRegistry(openTelemetry: OpenTelemetry): ObservationRegistry {
    val tracer = OtelTracer(
        openTelemetry.getTracer("mongodb-driver"),
        OtelCurrentTraceContext(),
    ) { /* We do not need Micrometer's event publishing. */ }

    return ObservationRegistry.create().apply {
        observationConfig().observationHandler(DefaultTracingObservationHandler(tracer))
    }
}
