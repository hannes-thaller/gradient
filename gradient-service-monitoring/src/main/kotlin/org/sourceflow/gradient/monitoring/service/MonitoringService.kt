package org.sourceflow.gradient.monitoring.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import org.sourceflow.gradient.monitoring.entity.LinkedFrame
import org.sourceflow.gradient.monitoring.persistence.MonitoringDao
import org.sourceflow.gradient.monitoring.persistence.ProtobufSerde
import org.sourceflow.gradient.monitoring.services.MonitoringServiceGrpcKt
import java.io.Closeable


class MonitoringService(
    client: PulsarClient,
    private val monitoringDao: MonitoringDao
) : MonitoringServiceGrpcKt.MonitoringServiceCoroutineImplBase(), Closeable {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val producer = client.newProducer(Schema.PROTOBUF(MonitoringEntities.MonitoringMessage::class.java))
        .topic("frame")
        .create()
    private val consumer = client.newConsumer(Schema.PROTOBUF(MonitoringEntities.MonitoringMessage::class.java))
        .topic("monitoring")
        .subscriptionName("gs-monitoring-service")
        .messageListener(MonitoringServiceSubscriber())
        .subscribe()

    /**
     * Monitoring message rely on message order
     */
    inner class MonitoringServiceSubscriber : MessageListener<MonitoringEntities.MonitoringMessage> {
        private val accumulators = mutableMapOf<CommonEntities.ProjectContext, FrameAccumulator>()

        override fun received(
            consumer: Consumer<MonitoringEntities.MonitoringMessage>,
            msg: Message<MonitoringEntities.MonitoringMessage>
        ) = runBlocking<Unit> {
            val monitoringMessage = msg.value
            when (monitoringMessage.payloadCase) {
                MonitoringEntities.MonitoringMessage.PayloadCase.MONITORING_STREAM_DETAIL -> {
                    handleMonitoringMessage(monitoringMessage)
                    consumer.acknowledgeAsync(msg).await()
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                    consumer.acknowledgeAsync(msg).await()
                }
            }
        }

        private suspend fun handleMonitoringMessage(msg: MonitoringEntities.MonitoringMessage) = coroutineScope {
            with(msg) {
                require(hasMonitoringStreamDetail())
                logger.debug { "Received ${monitoringStreamDetail.eventsCount} events @ ${projectContext.toSimpleString()} (${monitoringStreamDetail.control.type})" }


                val accumulator = when (monitoringStreamDetail.control.type) {
                    CommonEntities.ControlType.OPEN -> {
                        require(projectContext !in accumulators)
                        FrameAccumulator()
                            .also { accumulators[projectContext] = it }
                    }
                    CommonEntities.ControlType.HEARTBEAT -> {
                        require(projectContext in accumulators)
                        //TODO add unspooling if not present
                        accumulators[projectContext]!!
                    }
                    CommonEntities.ControlType.CLOSE -> {
                        require(projectContext in accumulators)
                        // TODO remove spooled states
                        accumulators.remove(projectContext)!!
                    }
                    else -> error("Unknown stream state")
                }

                val frames = accumulator.accumulate(monitoringStreamDetail.eventsList)

                if (frames.isNotEmpty()) {
                    launch { monitoringDao.saveFrames(projectContext, frames) }
                }
                launch {
                    producer.sendAsync(
                        createFrameMessage(
                            projectContext,
                            monitoringStreamDetail.control.type,
                            frames,
                            accumulator.closedFrames
                        )
                    ).await()
                }
            }
        }
    }

    private fun createFrameMessage(
        projectContext: CommonEntities.ProjectContext,
        controlType: CommonEntities.ControlType,
        frames: List<LinkedFrame>,
        sendFrames: Long
    ): MonitoringEntities.MonitoringMessage {
        return MonitoringEntities.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setFrameStreamDetail(
                MonitoringEntities.FrameStreamDetail.newBuilder()
                    .setControl(
                        CommonEntities.StreamControl.newBuilder()
                            .setType(controlType)
                            .setSendMessages(sendFrames)
                    )
                    .addAllFrames(frames.map { ProtobufSerde.to(it) })
            )
            .build()
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        consumer.unsubscribe()
        producer.close()
        consumer.close()
    }
}