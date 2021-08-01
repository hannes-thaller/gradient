package org.sourceflow.gradient.project.service

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.introspect.entities.IntrospectEntities
import org.sourceflow.gradient.project.persistence.ProjectDao
import org.sourceflow.gradient.project.services.ProjectServiceGrpcKt
import java.io.Closeable

private val logger = KotlinLogging.logger {}

internal class IntrospectService(
    client: PulsarClient,
    private val projectDao: ProjectDao
) : ProjectServiceGrpcKt.ProjectServiceCoroutineImplBase(), Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(IntrospectEntities.IntrospectMessage::class.java))
        .topic("introspect")
        .subscriptionName("gs-project-service")
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
            projectDao.reset()
        }
    }

    override fun close() {
        logger.debug { "Closing consumers" }

        consumer.unsubscribe()
        consumer.close()
    }
}