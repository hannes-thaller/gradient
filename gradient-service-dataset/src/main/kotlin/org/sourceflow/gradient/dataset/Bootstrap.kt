package org.sourceflow.gradient.dataset

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger { }

fun main() = runBlocking {
    logConfiguration()

    withContext(Dispatchers.IO) {
        val server = DIContainer.grpcServer
        server.start()
        server.awaitTermination()
    }
}

private fun logConfiguration() {
    val location = if (DIContainer.inContainer) "in container" else "locally"
    logger.info { "Dataset service running $location" }
    logger.info { "GRPC at ${DIContainer.grpcPort}" }
    logger.info { "Pulsar at ${DIContainer.pulsarURL}" }
    logger.info { "MongoDB at ${DIContainer.mongoURL}" }
}