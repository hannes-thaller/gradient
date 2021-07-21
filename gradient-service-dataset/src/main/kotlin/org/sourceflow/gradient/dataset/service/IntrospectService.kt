package org.sourceflow.gradient.dataset.service

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.code.CodeServiceGrpcKt
import org.sourceflow.gradient.dataset.persistence.DatasetDao
import org.sourceflow.gradient.introspect.IntrospectEntity
import java.io.Closeable

private val logger = KotlinLogging.logger {}

internal class IntrospectService(client: PulsarClient,
                                 private val datasetDao: DatasetDao) : CodeServiceGrpcKt.CodeServiceCoroutineImplBase(), Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(IntrospectEntity.IntrospectMessage::class.java))
            .topic("introspect")
            .subscriptionName("gs-dataset-service")
            .messageListener(IntrospectSubscriber())
            .subscribe()


    inner class IntrospectSubscriber : MessageListener<IntrospectEntity.IntrospectMessage> {
        override fun received(consumer: Consumer<IntrospectEntity.IntrospectMessage>, msg: Message<IntrospectEntity.IntrospectMessage>) = runBlocking<Unit> {
            val introspectMessage = msg.value

            when (introspectMessage.action) {
                IntrospectEntity.Action.RESET -> resetPersistence()
                IntrospectEntity.Action.UNRECOGNIZED -> logger.warn { "Unrecognized operation" }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                }
            }
        }

        private suspend fun resetPersistence() {
            datasetDao.reset()
        }
    }

    override fun close() {
        logger.debug { "Closing consumers" }

        consumer.unsubscribe()
        consumer.close()
    }
}