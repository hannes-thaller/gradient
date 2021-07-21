package org.sourceflow.gradient.dataset.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.code.CodeEntity
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.dataset.DatasetEntity
import org.sourceflow.gradient.dataset.DatasetServiceGrpcKt
import org.sourceflow.gradient.dataset.entity.DatasetHandle
import org.sourceflow.gradient.dataset.entity.FeatureDescription
import org.sourceflow.gradient.dataset.persistence.DatasetDao
import org.sourceflow.gradient.dataset.persistence.DatasetEntitySerde
import org.sourceflow.gradient.monitoring.MonitoringEntity
import org.sourceflow.pulsar.acknowledgeSuspend
import org.sourceflow.pulsar.sendSuspend
import java.io.Closeable
import kotlin.random.Random

class DatasetService(client: PulsarClient,
                     private val datasetSize: Int,
                     private val datasetDao: DatasetDao) : DatasetServiceGrpcKt.DatasetServiceCoroutineImplBase(), Closeable {

    companion object {
        private val logger = KotlinLogging.logger {}
        const val maximumLoadedIds = 1_000_000 // TODO injector
        val random = Random(14)
        val creationExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println(throwable)
        }
    }

    private val codeConsumer = client.newConsumer(Schema.PROTOBUF(CodeEntity.CodeMessage::class.java))
            .topic("code")
            .subscriptionName("gs-dataset-service")
            .messageListener(CodeSubscriber())
            .subscribe()

    private val monitoringConsumer = client.newConsumer(Schema.PROTOBUF(MonitoringEntity.MonitoringMessage::class.java))
            .topic("frame")
            .subscriptionName("gs-dataset-service")
            .messageListener(MonitoringSubscriber())
            .subscribe()
    private val datasetProducer = client.newProducer(Schema.PROTOBUF(DatasetEntity.DatasetMessage::class.java))
            .topic("dataset")
            .create()

    private val datasetCreationChannel: Channel<CommonEntity.ProjectContext> = Channel()

    init {
        CoroutineScope(context).launch {
            // TODO inject amount
            // TODO how does the cancellation work
            repeat(1) { launchDatasetBuilder(it, datasetCreationChannel) }
        }
    }

    private fun CoroutineScope.launchDatasetBuilder(id: Int, channel: ReceiveChannel<CommonEntity.ProjectContext>) = launch {
        logger.debug { "Dataset builder launched ($id)" }

        for (projectContext in channel) {
            logger.debug { "Preparing datasets for ${projectContext.toSimpleString()}" }
            for (featureDescription in datasetDao.loadFeatureDescriptions(projectContext)) {
                createDataset(projectContext, featureDescription)
            }
        }

        logger.debug { "Dataset builder exiting ($id)" }
    }

    fun CoroutineScope.createDataset(projectContext: CommonEntity.ProjectContext, featureDescription: FeatureDescription) = launch(creationExceptionHandler) {
        logger.debug { "Creating dataset ${projectContext.toSimpleString()}, elementId=${featureDescription.elementId}" }

        val ids = datasetDao.loadDatapointIds(CommonEntitySerde.to(projectContext.sessionId), featureDescription, maximumLoadedIds)

        val selectedIds = ids.indices
                .shuffled(random)
                .take(datasetSize)
                .map { ids[it] }

        val datasetHandle = DatasetHandle(featureDescription, selectedIds, CommonEntitySerde.to(projectContext.sessionId))
        launch { datasetDao.saveDatasetHandle(datasetHandle) }
        launch { datasetProducer.sendSuspend(createDatasetHandleMessage(projectContext, datasetHandle)) }
    }

    private fun createDatasetHandleMessage(projectContext: CommonEntity.ProjectContext,
                                           datasetHandle: DatasetHandle): DatasetEntity.DatasetMessage {
        return DatasetEntity.DatasetMessage.newBuilder()
                .setProjectContext(projectContext)
                .setDatasetHandleDetail(DatasetEntitySerde.from(datasetHandle))
                .build()
    }

    override suspend fun loadDataset(request: DatasetEntity.DatasetMessage): DatasetEntity.DatasetMessage {
        require(request.hasDatasetHandleDetail()) { "Dataset handle detail required" }

        return with(request.datasetHandleDetail) {
            val ids = datapointIdsList.map { CommonEntitySerde.to(it) }
            val datapoints = datasetDao.loadDatapoints(ids)
                    .map { DatasetEntitySerde.from(it) }

            DatasetEntity.DatasetMessage.newBuilder()
                    .setProjectContext(request.projectContext)
                    .setDatasetDetail(DatasetEntity.DatasetDetail.newBuilder()
                            .setDatasetId(datasetId)
                            .setFeatureDescription(featureDescription)
                            .addAllDatapoints(datapoints))
                    .build()
        }
    }

    inner class CodeSubscriber : MessageListener<CodeEntity.CodeMessage> {
        override fun received(consumer: Consumer<CodeEntity.CodeMessage>, msg: Message<CodeEntity.CodeMessage>) {
            CoroutineScope(context).launch {
                val codeMessage = msg.value
                when (codeMessage.payloadCase) {
                    CodeEntity.CodeMessage.PayloadCase.PROGRAM_DETAIL -> {
                        handleProgramMessage(codeMessage)
                    }
                    else -> {
                        logger.debug { "Acknowledging irrelevant message" }
                    }
                }
                consumer.acknowledgeSuspend(msg)
            }
        }

        private suspend fun handleProgramMessage(msg: CodeEntity.CodeMessage): Unit = coroutineScope<Unit> {
            with(msg) {
                require(hasProgramDetail())
                logger.debug { "Receiving new program ${projectContext.toSimpleString()}" }

                val featureDescriptions = FeatureDescriptionBuilder(CommonEntitySerde.to(projectContext.projectId), programDetail)
                        .build()
                launch { datasetDao.saveFeatureDescription(featureDescriptions) }

                val dataMessage = DatasetEntity.DatasetMessage.newBuilder()
                        .setProjectContext(projectContext)
                        .setFeatureDescriptionsDetail(DatasetEntitySerde.to(featureDescriptions))
                        .build()

                launch { datasetProducer.sendSuspend(dataMessage) }
            }
        }
    }

    inner class MonitoringSubscriber : MessageListener<MonitoringEntity.MonitoringMessage> {
        private val factories = mutableMapOf<CommonEntity.ProjectContext, DatapointFactory>()

        override fun received(consumer: Consumer<MonitoringEntity.MonitoringMessage>, msg: Message<MonitoringEntity.MonitoringMessage>) = runBlocking {
            val monitoringMessage = msg.value
            when (monitoringMessage.payloadCase) {
                MonitoringEntity.MonitoringMessage.PayloadCase.FRAME_STREAM_DETAIL -> {
                    handleFrameMessageDetail(monitoringMessage)
                    consumer.acknowledgeSuspend(msg)
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                    consumer.acknowledgeSuspend(msg)
                }
            }
        }

        private suspend fun handleFrameMessageDetail(msg: MonitoringEntity.MonitoringMessage) = coroutineScope {
            with(msg) {
                require(hasFrameStreamDetail())
                logger.debug { "Received ${frameStreamDetail.framesCount} frames @ ${projectContext.toSimpleString()} (${frameStreamDetail.control.type})" }

                val factory = when (frameStreamDetail.control.type) {
                    CommonEntity.ControlType.OPEN -> {
                        require(projectContext !in factories)
                        val featureDescriptions = datasetDao.loadFeatureDescriptions(projectContext)
                        DatapointFactory(CommonEntitySerde.to(projectContext.sessionId), featureDescriptions)
                                .also { factories[projectContext] = it }
                    }
                    CommonEntity.ControlType.HEARTBEAT -> {
                        require(projectContext in factories)
                        factories[projectContext]!!
                    }
                    CommonEntity.ControlType.CLOSE -> {
                        require(projectContext in factories)
                        factories.remove(projectContext)!!
                    }
                    else -> error("Unknown stream state")
                }

                val datapoints = factory.createDatapoints(frameStreamDetail.framesList)
                if (datapoints.isNotEmpty()) {
                    datasetDao.saveDatapoints(datapoints)
                }

                // TODO add retention policy for factories, spool them after x time inactive, clean them up after y time spooled
                // TODO add configuration for automatic dataset creation
                if (frameStreamDetail.hasControl() &&
                        frameStreamDetail.control.type == CommonEntity.ControlType.CLOSE) {
                    logger.debug { "Stream of frames ended. Issuing dataset creation ${projectContext.toSimpleString()}" }

                    factories.remove(projectContext)
                    datasetCreationChannel.send(projectContext)
                }
            }
        }
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        codeConsumer.unsubscribe()
        codeConsumer.close()
        monitoringConsumer.unsubscribe()
        monitoringConsumer.close()
        datasetProducer.close()
    }
}