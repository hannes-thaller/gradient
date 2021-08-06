package org.sourceflow.gradient.dataset

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.bson.UuidRepresentation
import org.sourceflow.gradient.dataset.persistence.DatasetDao
import org.sourceflow.gradient.dataset.service.DatasetService
import org.sourceflow.gradient.dataset.service.IntrospectService
import kotlin.concurrent.thread

object DIContainer {
    private val cleanRoutines = mutableListOf<Pair<Int, () -> Unit>>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(false) {
            cleanRoutines.sortedByDescending { it.first }
                    .map { it.second.invoke() }
        })
    }

    val inContainer: Boolean = System.getenv("GRADIENT_DOCKER") != null

    const val grpcPort: Int = 13001

    val pulsarURL: String by lazy {
        if (inContainer) {
            "pulsar://gs-message-database:6650"
        } else {
            "pulsar://localhost:10002"
        }
    }

    val mongoURL: String by lazy {
        if (inContainer) {
            "mongodb://gs-dataset-database:27017"
        } else {
            "mongodb://localhost:13002"
        }
    }

    val pulsarClient: PulsarClient by lazy {
        PulsarClient.builder()
                .serviceUrl(pulsarURL)
                .build()
                .also { cleanRoutines.add(1 to it::close) }
    }

    val mongoSettings: MongoClientSettings by lazy {
        MongoClientSettings.builder()
                .applyConnectionString(ConnectionString(mongoURL))
                .apply {
                    uuidRepresentation(UuidRepresentation.STANDARD)
                }
                .build()
    }

    val mongoClient: MongoClient by lazy {
        MongoClients.create(mongoSettings)
                .also { cleanRoutines.add(2 to it::close) }
    }

    val datasetDao: DatasetDao by lazy {
        DatasetDao(mongoClient)
    }

    val datasetSize: Int by lazy {
        10_000
    }

    val datasetService: DatasetService by lazy {
        DatasetService(pulsarClient, datasetSize, datasetDao)
                .also { cleanRoutines.add(2 to it::close) }
    }

    internal val introspectService: IntrospectService by lazy {
        IntrospectService(pulsarClient, datasetDao)
                .also { cleanRoutines.add(2 to it::close) }
    }


    val grpcServer: Server by lazy {
        ServerBuilder.forPort(grpcPort)
                .addService(datasetService)
                .build()
    }
}