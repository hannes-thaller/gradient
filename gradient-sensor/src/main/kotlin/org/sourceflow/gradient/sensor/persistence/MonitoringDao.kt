package org.sourceflow.gradient.sensor.persistence

import mu.KotlinLogging
import org.apache.pulsar.client.api.PulsarClient
import org.apache.pulsar.client.api.Schema
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.monitoring.MonitoringEntity
import java.io.Closeable
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger { }


class MonitoringDao(
    private val batchSize: Int,
    instanceName: String,
    client: PulsarClient
) : Closeable {

    private val producer = client.newProducer(Schema.PROTOBUF(MonitoringEntity.MonitoringMessage::class.java))
        .topic("monitoring")
        .producerName(instanceName)
        .blockIfQueueFull(true)
        .create()

    private val buffer = mutableListOf<MonitoringEntity.MonitoringEvent>()

    private var projectContext: CommonEntity.ProjectContext? = null
    private var sendMessages = AtomicLong(0)

    fun getMessagesSend(): Long {
        return sendMessages.get()
    }

    fun reportOn(projectContext: CommonEntity.ProjectContext) {
        this.projectContext?.let { flush() }
        this.projectContext = projectContext

        logger.debug { "Opened monitoring stream ${projectContext.toSimpleString()}" }
        producer.send(createControlMessage(projectContext, CommonEntity.ControlType.OPEN, 0))
    }

    fun reportStop() {
        projectContext?.let {
            logger.debug { "Closed monitoring stream $it" }
            flush()
            producer.send(createControlMessage(it, CommonEntity.ControlType.CLOSE, sendMessages.get()))
            producer.flush()
            this.projectContext = null
        }
    }

    fun reportEvent(event: MonitoringEntity.MonitoringEvent) {
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
        projectContext: CommonEntity.ProjectContext,
        controlType: CommonEntity.ControlType,
        sendMessages: Long
    ): MonitoringEntity.MonitoringMessage {
        return MonitoringEntity.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(
                MonitoringEntity.MonitoringStreamDetail.newBuilder()
                    .setControl(
                        CommonEntity.StreamControl.newBuilder()
                            .setType(controlType)
                            .setSendMessages(sendMessages)
                    )
            )
            .build()
    }

    private fun createMonitoringMessage(
        projectContext: CommonEntity.ProjectContext,
        events: List<MonitoringEntity.MonitoringEvent>,
        sendCount: Long
    ): MonitoringEntity.MonitoringMessage {
        return MonitoringEntity.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(
                MonitoringEntity.MonitoringStreamDetail.newBuilder()
                    .setControl(
                        CommonEntity.StreamControl.newBuilder()
                            .setType(CommonEntity.ControlType.HEARTBEAT)
                            .setSendMessages(sendCount)
                    )
                    .addAllEvents(events)
            )
            .build()
    }
}