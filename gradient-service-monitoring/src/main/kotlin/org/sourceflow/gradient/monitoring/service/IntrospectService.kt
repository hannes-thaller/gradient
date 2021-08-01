package org.sourceflow.gradient.monitoring.service

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.introspect.entities.IntrospectEntities
import org.sourceflow.gradient.monitoring.persistence.MonitoringDao
import org.sourceflow.gradient.monitoring.services.MonitoringServiceGrpcKt
import java.io.Closeable

private val logger = KotlinLogging.logger {}

internal class IntrospectService(
    client: PulsarClient,
    private val monitoringDao: MonitoringDao
) : MonitoringServiceGrpcKt.MonitoringServiceCoroutineImplBase(), Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(IntrospectEntities.IntrospectMessage::class.java))
        .topic("introspect")
        .subscriptionName("gs-monitoring-service")
        .messageListener(IntrospectSubscriber())
        .subscribe()


    inner class IntrospectSubscriber : MessageListener<IntrospectEntities.IntrospectMessage> {
        override fun received(
            consumer: Consumer<IntrospectEntities.IntrospectMessage>,
            msg: Message<IntrospectEntities.IntrospectMessage>
        ) = runBlocking<Unit> {
            val introspectMessage = msg.value

            when (introspectMessage.action) {
                IntrospectEntities.Action.RESET -> resetPersistence()
                IntrospectEntities.Action.UNRECOGNIZED -> logger.warn { "Unrecognized operation" }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                }
            }
        }

        private suspend fun resetPersistence() {
            monitoringDao.reset()
        }
    }

    override fun close() {
        logger.debug { "Closing consumers" }

        consumer.unsubscribe()
        consumer.close()
    }
}