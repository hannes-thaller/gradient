package org.sourceflow.gradient.code

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.bson.UuidRepresentation
import org.sourceflow.gradient.code.persistence.CodeDao
import org.sourceflow.gradient.code.service.CodeService
import org.sourceflow.gradient.code.service.IntrospectService
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

    const val grpcPort: Int = 12001

    val pulsarURL: String by lazy {
        if (inContainer) {
            "pulsar://gs-message-database:6650"
        } else {
            "pulsar://localhost:10002"
        }
    }

    val mongoURL: String by lazy {
        if (inContainer) {
            "mongodb://gs-code-database:27017"
        } else {
            "mongodb://localhost:12002"
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
                .also { cleanRoutines.add(1 to it::close) }
    }

    val codeDao: CodeDao by lazy {
        CodeDao(mongoClient)
    }

    val codeService: CodeService by lazy {
        CodeService(pulsarClient, codeDao)
                .also { cleanRoutines.add(2 to it::close) }
    }

    internal val introspectService: IntrospectService by lazy {
        IntrospectService(pulsarClient, codeDao)
                .also { cleanRoutines.add(2 to it::close) }
    }


    val grpcServer: Server by lazy {
        ServerBuilder.forPort(grpcPort)
                .addService(codeService)
                .addService(introspectService)
                .build()
    }
}