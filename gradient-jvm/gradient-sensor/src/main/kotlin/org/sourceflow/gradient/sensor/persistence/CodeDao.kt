package org.sourceflow.gradient.sensor.persistence


import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.sensor.entity.CodeElementGraph
import java.io.Closeable
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class CodeDao(
    client: PulsarClient,
    instanceName: String
) : Closeable {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val producer = client.newProducer(Schema.PROTOBUF(CodeEntities.CodeMessage::class.java))
        .topic("code")
        .producerName(instanceName)
        .create()
    private val consumer = client.newConsumer(Schema.PROTOBUF(CodeEntities.CodeMessage::class.java))
        .topic("code")
        .subscriptionName(instanceName)
        .subscribe()


    @OptIn(ExperimentalTime::class)
    suspend fun reportElementGraph(
        project: CommonEntities.ProjectContext,
        elementGraph: CodeElementGraph
    ): MutableList<CodeEntities.CodeElementModelUpdate> {
        assert(project.hasProjectId())
        assert(project.hasSessionId())
        val request = CodeEntities.CodeMessage.newBuilder()
            .setProjectContext(project)
            .setProgramDetail(
                CodeEntities.ProgramDetail.newBuilder()
                    .addAllTypes(elementGraph.types.map { GrpcSerde.convert(it) })
                    .addAllProperties(elementGraph.properties.map { GrpcSerde.convert(it) })
                    .addAllExecutables(elementGraph.executables.map { GrpcSerde.convert(it) })
                    .addAllParameters(elementGraph.parameters.map { GrpcSerde.convert(it) })
                    .build()
            )
            .build()


        logger.debug { "Sending program elements: ${CommonEntitySerde.toUUID(project.projectId)}" }
        producer.sendAsync(request).await()

        return withTimeout(Duration.Companion.seconds(10)) {
            var msg = consumer.receiveAsync().await()
            while (!(msg.value.projectContext == request.projectContext
                        && msg.value.hasModelUpdateDetail())
            ) {
                msg = consumer.receiveAsync().await()
                consumer.acknowledgeAsync(msg)
            }

            logger.debug { "Received ${msg.value.modelUpdateDetail.updatesCount} modeling universe element updates " }

            msg.value.modelUpdateDetail.updatesList
        }
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        consumer.unsubscribe()
        consumer.close()
        producer.close()
    }
}
