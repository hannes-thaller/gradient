package org.sourceflow.gradient.project

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import io.grpc.Server
import io.grpc.ServerBuilder
import org.apache.pulsar.client.api.PulsarClient
import org.bson.UuidRepresentation
import org.sourceflow.gradient.project.persistence.ProjectDao
import org.sourceflow.gradient.project.service.IntrospectService
import org.sourceflow.gradient.project.service.ProjectService
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

    val pulsarURL: String by lazy {
        if (inContainer) {
            "pulsar://gs-message-database:6650"
        } else {
            "pulsar://localhost:10002"
        }
    }

    val mongoURL: String by lazy {
        if (inContainer) {
            "mongodb://gs-project-database:27017"
        } else {
            "mongodb://localhost:11002"
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

    val projectDao: ProjectDao by lazy {
        ProjectDao(mongoClient)
    }

    val projectService: ProjectService by lazy {
        ProjectService(pulsarClient, projectDao)
                .also { cleanRoutines.add(2 to it::close) }
    }

    internal val introspectService: IntrospectService by lazy {
        IntrospectService(pulsarClient, projectDao)
                .also { cleanRoutines.add(2 to it::close) }
    }

    const val grpcPort: Int = 11001

    val grpcServer: Server by lazy {
        ServerBuilder.forPort(grpcPort)
                .addService(projectService)
                .addService(introspectService)
                .build()
    }
}
