package org.sourceflow.gradient.sensor

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.sourceflow.gradient.sensor.persistence.CodeDao
import org.sourceflow.gradient.sensor.persistence.MonitoringDao
import org.sourceflow.gradient.sensor.persistence.ProjectDao
import java.util.*
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger { }


/**
 * A minimal named DI Container
 */
object DIContainer {
    private val cleanRoutines = mutableListOf<Pair<Int, () -> Unit>>()

    init {
        Runtime.getRuntime().addShutdownHook(thread(false) {
            logger.debug { "Cleaning Gradient resources." }
            cleanRoutines.sortedByDescending { it.first }
                    .map { it.second.invoke() }
        })
    }

    val instanceName: String by lazy {
        "gradient-sensor-jvm-${UUID.randomUUID()}"
    }

    val inContainer: Boolean = System.getenv("GRADIENT_DOCKER") != null

    val grpcURL: Pair<String, Int> by lazy {
        if (inContainer) {
            "gs-monitoring-service" to 14001
        } else {
            "localhost" to 14001
        }
    }

    val grpcChannel: ManagedChannel by lazy {
        ManagedChannelBuilder
                .forAddress(grpcURL.first, grpcURL.second)
                .usePlaintext()
                .build()
                .also { channel ->
                    val shutdown: () -> Unit = { channel.shutdown() }
                    cleanRoutines.add(1 to shutdown)
                }
    }


    val pulsarURL: String by lazy {
        if (inContainer) {
            "pulsar://gs-message-database:6650"
        } else {
            "pulsar://localhost:10002"
        }
    }

    val pulsarClient: PulsarClient by lazy {
        PulsarClient.builder()
                .serviceUrl(pulsarURL)
                .ioThreads(3)
                .build()
                .also { cleanRoutines.add(1 to it::close) }
    }


    val projectDao: ProjectDao by lazy {
        ProjectDao(pulsarClient, instanceName)
                .also { cleanRoutines.add(2 to it::close) }
    }
    val codeDao: CodeDao by lazy {
        CodeDao(pulsarClient, instanceName)
                .also { cleanRoutines.add(2 to it::close) }
    }
    val batchSize: Int by lazy {
        2048
    }

    val monitoringDao: MonitoringDao by lazy {
        MonitoringDao(batchSize, instanceName, pulsarClient)
                .also { cleanRoutines.add(2 to it::close) }
    }

    val gradientAgent: GradientAgent by lazy {
        GradientAgent(projectDao, codeDao, monitoringDao)
    }
}