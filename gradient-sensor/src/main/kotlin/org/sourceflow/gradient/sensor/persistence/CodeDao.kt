package org.sourceflow.gradient.sensor.persistence

import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.withTimeout
import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.code.CodeEntity
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.sensor.entity.CodeElementGraph
import org.sourceflow.pulsar.acknowledgeSuspend
import org.sourceflow.pulsar.receiveAnswerSuspend
import org.sourceflow.pulsar.sendSuspend
import java.io.Closeable
import java.io.File
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

class CodeDao(
    client: PulsarClient,
    instanceName: String
) : Closeable {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val producer = client.newProducer(Schema.PROTOBUF(CodeEntity.CodeMessage::class.java))
        .topic("code")
        .producerName(instanceName)
        .create()
    private val consumer = client.newConsumer(Schema.PROTOBUF(CodeEntity.CodeMessage::class.java))
        .topic("code")
        .subscriptionName(instanceName)
        .subscribe()


    @OptIn(ExperimentalTime::class)
    suspend fun reportElementGraph(
        project: CommonEntity.ProjectContext,
        elementGraph: CodeElementGraph
    ): MutableList<CodeEntity.CodeElementModelUpdate> {
        assert(project.hasProjectId())
        assert(project.hasSessionId())
        val request = CodeEntity.CodeMessage.newBuilder()
            .setProjectContext(project)
            .setProgramDetail(
                CodeEntity.ProgramDetail.newBuilder()
                    .addAllTypes(elementGraph.types.map { GrpcSerde.convert(it) })
                    .addAllProperties(elementGraph.properties.map { GrpcSerde.convert(it) })
                    .addAllExecutables(elementGraph.executables.map { GrpcSerde.convert(it) })
                    .addAllParameters(elementGraph.parameters.map { GrpcSerde.convert(it) })
                    .build()
            )
            .build()


        logger.debug { "Sending program elements: ${CommonEntitySerde.to(project.projectId)}" }
        producer.sendSuspend(request)

        return withTimeout(10.seconds) {
            val msg = consumer.receiveAnswerSuspend {
                it.value.projectContext == request.projectContext
                        && it.value.hasModelUpdateDetail()
            }
            consumer.acknowledgeSuspend(msg)
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
