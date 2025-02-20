package org.sourceflow.gradient.sensor.persistence

import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.sensor.entity.CanonicalName
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.project.entities.ProjectEntities

import java.io.Closeable
import java.util.*

private val logger = KotlinLogging.logger { }

class ProjectDao(client: PulsarClient, instanceName: String) : Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(ProjectEntities.ProjectMessage::class.java))
        .topic("project")
        .subscriptionName(instanceName)
        .subscribe()
    private val producer = client.newProducer(Schema.PROTOBUF(ProjectEntities.ProjectMessage::class.java))
        .topic("project")
        .producerName(instanceName)
        .create()

    suspend fun registerProject(name: CanonicalName): CommonEntities.ProjectContext {
        logger.debug { "Registering project ${name.components.joinToString(".") { it.value }}" }

        val registration = ProjectEntities.ProjectMessage.newBuilder()
            .setRequestId(CommonEntitySerde.fromUUID(UUID.randomUUID()))
            .setName(GrpcSerde.convert(name))
            .build()
        producer.sendAsync(registration).await()

        var msg = consumer.receiveAsync().await()
        consumer.acknowledgeAsync(msg)
        while (msg.value.requestId == registration.requestId && msg.value.hasContext()) {
            msg = consumer.receiveAsync().await()
            consumer.acknowledgeAsync(msg)
        }

        return msg.value.context
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        consumer.unsubscribe()
        consumer.close()
        producer.close()
    }
}