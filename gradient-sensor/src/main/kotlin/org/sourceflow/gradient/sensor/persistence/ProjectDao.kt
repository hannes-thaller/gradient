package org.sourceflow.gradient.sensor.persistence

import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.project.ProjectEntity
import org.sourceflow.gradient.sensor.entity.CanonicalName
import org.sourceflow.pulsar.acknowledgeSuspend
import org.sourceflow.pulsar.receiveAnswerSuspend
import org.sourceflow.pulsar.sendSuspend
import java.io.Closeable
import java.util.*

private val logger = KotlinLogging.logger { }

class ProjectDao(client: PulsarClient, instanceName: String) : Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(ProjectEntity.ProjectMessage::class.java))
        .topic("project")
        .subscriptionName(instanceName)
        .subscribe()
    private val producer = client.newProducer(Schema.PROTOBUF(ProjectEntity.ProjectMessage::class.java))
        .topic("project")
        .producerName(instanceName)
        .create()

    suspend fun registerProject(name: CanonicalName): CommonEntity.ProjectContext {
        logger.debug { "Registering project ${name.components.joinToString(".") { it.value }}" }

        val registration = ProjectEntity.ProjectMessage.newBuilder()
            .setRequestId(CommonEntitySerde.from(UUID.randomUUID()))
            .setName(GrpcSerde.convert(name))
            .build()
        producer.sendSuspend(registration)

        val confirmation = consumer.receiveAnswerSuspend {
            it.value.requestId == registration.requestId && it.value.hasContext()
        }
        consumer.acknowledgeSuspend(confirmation)

        return confirmation.value.context
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        consumer.unsubscribe()
        consumer.close()
        producer.close()
    }
}