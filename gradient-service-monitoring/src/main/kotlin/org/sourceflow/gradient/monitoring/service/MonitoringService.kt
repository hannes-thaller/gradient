package org.sourceflow.gradient.monitoring.service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.monitoring.MonitoringEntity
import org.sourceflow.gradient.monitoring.MonitoringServiceGrpcKt
import org.sourceflow.gradient.monitoring.entity.LinkedFrame
import org.sourceflow.gradient.monitoring.persistence.MonitoringDao
import org.sourceflow.gradient.monitoring.persistence.ProtobufSerde
import org.sourceflow.pulsar.acknowledgeSuspend
import org.sourceflow.pulsar.sendSuspend
import java.io.Closeable


class MonitoringService(client: PulsarClient,
                        private val monitoringDao: MonitoringDao)
    : MonitoringServiceGrpcKt.MonitoringServiceCoroutineImplBase(), Closeable {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val producer = client.newProducer(Schema.PROTOBUF(MonitoringEntity.MonitoringMessage::class.java))
            .topic("frame")
            .create()
    private val consumer = client.newConsumer(Schema.PROTOBUF(MonitoringEntity.MonitoringMessage::class.java))
            .topic("monitoring")
            .subscriptionName("gs-monitoring-service")
            .messageListener(MonitoringServiceSubscriber())
            .subscribe()

    /**
     * Monitoring message rely on message order
     */
    inner class MonitoringServiceSubscriber : MessageListener<MonitoringEntity.MonitoringMessage> {
        private val accumulators = mutableMapOf<CommonEntity.ProjectContext, FrameAccumulator>()

        override fun received(consumer: Consumer<MonitoringEntity.MonitoringMessage>, msg: Message<MonitoringEntity.MonitoringMessage>) = runBlocking<Unit> {
            val monitoringMessage = msg.value
            when (monitoringMessage.payloadCase) {
                MonitoringEntity.MonitoringMessage.PayloadCase.MONITORING_STREAM_DETAIL -> {
                    handleMonitoringMessage(monitoringMessage)
                    consumer.acknowledgeSuspend(msg)
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                    consumer.acknowledgeSuspend(msg)
                }
            }
        }

        private suspend fun handleMonitoringMessage(msg: MonitoringEntity.MonitoringMessage) = coroutineScope {
            with(msg) {
                require(hasMonitoringStreamDetail())
                logger.debug { "Received ${monitoringStreamDetail.eventsCount} events @ ${projectContext.toSimpleString()} (${monitoringStreamDetail.control.type})" }


                val accumulator = when (monitoringStreamDetail.control.type) {
                    CommonEntity.ControlType.OPEN -> {
                        require(projectContext !in accumulators)
                        FrameAccumulator()
                                .also { accumulators[projectContext] = it }
                    }
                    CommonEntity.ControlType.HEARTBEAT -> {
                        require(projectContext in accumulators)
                        //TODO add unspooling if not present
                        accumulators[projectContext]!!
                    }
                    CommonEntity.ControlType.CLOSE -> {
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
                    producer.sendSuspend(createFrameMessage(projectContext, monitoringStreamDetail.control.type, frames, accumulator.closedFrames))
                }
            }
        }
    }

    private fun createFrameMessage(projectContext: CommonEntity.ProjectContext,
                                   controlType: CommonEntity.ControlType,
                                   frames: List<LinkedFrame>,
                                   sendFrames: Long): MonitoringEntity.MonitoringMessage {
        return MonitoringEntity.MonitoringMessage.newBuilder()
                .setProjectContext(projectContext)
                .setFrameStreamDetail(
                        MonitoringEntity.FrameStreamDetail.newBuilder()
                                .setControl(
                                        CommonEntity.StreamControl.newBuilder()
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