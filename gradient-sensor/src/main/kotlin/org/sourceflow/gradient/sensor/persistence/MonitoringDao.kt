package org.sourceflow.gradient.sensor.persistence

import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger { }


class MonitoringDao(
    private val batchSize: Int,
    instanceName: String,
    client: PulsarClient
) : Closeable {

    private val producer = client.newProducer(Schema.PROTOBUF(MonitoringEntities.MonitoringMessage::class.java))
        .topic("monitoring")
        .producerName(instanceName)
        .blockIfQueueFull(true)
        .create()

    private val buffer = mutableListOf<MonitoringEntities.MonitoringEvent>()

    private var projectContext: CommonEntities.ProjectContext? = null
    private var sendMessages = AtomicLong(0)

    fun getMessagesSend(): Long {
        return sendMessages.get()
    }

    fun reportOn(projectContext: CommonEntities.ProjectContext) {
        this.projectContext?.let { flush() }
        this.projectContext = projectContext

        logger.debug { "Opened monitoring stream ${projectContext.toSimpleString()}" }
        producer.send(createControlMessage(projectContext, CommonEntities.ControlType.OPEN, 0))
    }

    fun reportStop() {
        projectContext?.let {
            logger.debug { "Closed monitoring stream $it" }
            flush()
            producer.send(createControlMessage(it, CommonEntities.ControlType.CLOSE, sendMessages.get()))
            producer.flush()
            this.projectContext = null
        }
    }

    fun reportEvent(event: MonitoringEntities.MonitoringEvent) {
        projectContext?.let {
            buffer.add(event)
            if (buffer.size >= batchSize) {
                flush()
            }
        }
    }

    private fun flush() = synchronized(buffer) {
        projectContext?.let { ctx ->
            if (buffer.isNotEmpty()) {
                val sendCount = sendMessages.addAndGet(buffer.size.toLong())
                val msg = createMonitoringMessage(ctx, buffer, sendCount)
                buffer.clear()

                producer.sendAsync(msg).thenAccept {
                    logger.debug { "Sending event batch of $sendCount for ${ctx.toSimpleString()}" }
                }
            }
        }
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        reportStop()
        producer.close()
    }

    private fun createControlMessage(
        projectContext: CommonEntities.ProjectContext,
        controlType: CommonEntities.ControlType,
        sendMessages: Long
    ): MonitoringEntities.MonitoringMessage {
        return MonitoringEntities.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(
                MonitoringEntities.MonitoringStreamDetail.newBuilder()
                    .setControl(
                        CommonEntities.StreamControl.newBuilder()
                            .setType(controlType)
                            .setSendMessages(sendMessages)
                    )
            )
            .build()
    }

    private fun createMonitoringMessage(
        projectContext: CommonEntities.ProjectContext,
        events: List<MonitoringEntities.MonitoringEvent>,
        sendCount: Long
    ): MonitoringEntities.MonitoringMessage {
        return MonitoringEntities.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(
                MonitoringEntities.MonitoringStreamDetail.newBuilder()
                    .setControl(
                        CommonEntities.StreamControl.newBuilder()
                            .setType(CommonEntities.ControlType.HEARTBEAT)
                            .setSendMessages(sendCount)
                    )
                    .addAllEvents(events)
            )
            .build()
    }
}