package org.sourceflow.gradient.dataset.service

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.dataset.entities.DatasetEntities
import org.sourceflow.gradient.dataset.entity.DatasetHandle
import org.sourceflow.gradient.dataset.entity.FeatureDescription
import org.sourceflow.gradient.dataset.persistence.DatasetDao
import org.sourceflow.gradient.dataset.persistence.DatasetEntitySerde
import org.sourceflow.gradient.dataset.services.DatasetServiceGrpcKt
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import java.io.Closeable
import kotlin.random.Random

class DatasetService(
    client: PulsarClient,
    private val datasetSize: Int,
    private val datasetDao: DatasetDao
) : DatasetServiceGrpcKt.DatasetServiceCoroutineImplBase(), Closeable {

    companion object {
        private val logger = KotlinLogging.logger {}
        const val maximumLoadedIds = 1_000_000 // TODO injector
        val random = Random(14)
        val creationExceptionHandler = CoroutineExceptionHandler { _, throwable ->
            println(throwable)
        }
    }

    private val codeConsumer = client.newConsumer(Schema.PROTOBUF(CodeEntities.CodeMessage::class.java))
        .topic("code")
        .subscriptionName("gs-dataset-service")
        .messageListener(CodeSubscriber())
        .subscribe()

    private val monitoringConsumer =
        client.newConsumer(Schema.PROTOBUF(MonitoringEntities.MonitoringMessage::class.java))
            .topic("frame")
            .subscriptionName("gs-dataset-service")
            .messageListener(MonitoringSubscriber())
            .subscribe()
    private val datasetProducer = client.newProducer(Schema.PROTOBUF(DatasetEntities.DatasetMessage::class.java))
        .topic("dataset")
        .create()

    private val datasetCreationChannel: Channel<CommonEntities.ProjectContext> = Channel()

    init {
        CoroutineScope(context).launch {
            // TODO inject amount
            // TODO how does the cancellation work
            repeat(1) { launchDatasetBuilder(it, datasetCreationChannel) }
        }
    }

    private fun CoroutineScope.launchDatasetBuilder(id: Int, channel: ReceiveChannel<CommonEntities.ProjectContext>) =
        launch {
            logger.debug { "Dataset builder launched ($id)" }

            for (projectContext in channel) {
                logger.debug { "Preparing datasets for ${projectContext.toSimpleString()}" }
                for (featureDescription in datasetDao.loadFeatureDescriptions(projectContext)) {
                    createDataset(projectContext, featureDescription)
                }
            }

            logger.debug { "Dataset builder exiting ($id)" }
        }

    fun CoroutineScope.createDataset(
        projectContext: CommonEntities.ProjectContext,
        featureDescription: FeatureDescription
    ) = launch(creationExceptionHandler) {
        logger.debug { "Creating dataset ${projectContext.toSimpleString()}, elementId=${featureDescription.elementId}" }

        val ids = datasetDao.loadDatapointIds(
            CommonEntitySerde.toUUID(projectContext.sessionId),
            featureDescription,
            maximumLoadedIds
        )

        val selectedIds = ids.indices
            .shuffled(random)
            .take(datasetSize)
            .map { ids[it] }

        val datasetHandle =
            DatasetHandle(featureDescription, selectedIds, CommonEntitySerde.toUUID(projectContext.sessionId))
        launch { datasetDao.saveDatasetHandle(datasetHandle) }
        launch { datasetProducer.sendAsync(createDatasetHandleMessage(projectContext, datasetHandle)).await() }
    }

    private fun createDatasetHandleMessage(
        projectContext: CommonEntities.ProjectContext,
        datasetHandle: DatasetHandle
    ): DatasetEntities.DatasetMessage {
        return DatasetEntities.DatasetMessage.newBuilder()
            .setProjectContext(projectContext)
            .setDatasetHandleDetail(DatasetEntitySerde.from(datasetHandle))
            .build()
    }

    override suspend fun loadDataset(request: DatasetEntities.DatasetMessage): DatasetEntities.DatasetMessage {
        require(request.hasDatasetHandleDetail()) { "Dataset handle detail required" }

        return with(request.datasetHandleDetail) {
            val ids = datapointIdsList.map { CommonEntitySerde.toUUID(it) }
            val datapoints = datasetDao.loadDatapoints(ids)
                .map { DatasetEntitySerde.from(it) }

            DatasetEntities.DatasetMessage.newBuilder()
                .setProjectContext(request.projectContext)
                .setDatasetDetail(
                    DatasetEntities.DatasetDetail.newBuilder()
                        .setDatasetId(datasetId)
                        .setFeatureDescription(featureDescription)
                        .addAllDatapoints(datapoints)
                )
                .build()
        }
    }

    inner class CodeSubscriber : MessageListener<CodeEntities.CodeMessage> {
        override fun received(consumer: Consumer<CodeEntities.CodeMessage>, msg: Message<CodeEntities.CodeMessage>) {
            CoroutineScope(context).launch {
                val codeMessage = msg.value
                when (codeMessage.payloadCase) {
                    CodeEntities.CodeMessage.PayloadCase.PROGRAM_DETAIL -> {
                        handleProgramMessage(codeMessage)
                    }
                    else -> {
                        logger.debug { "Acknowledging irrelevant message" }
                    }
                }
                consumer.acknowledgeAsync(msg).await()
            }
        }

        private suspend fun handleProgramMessage(msg: CodeEntities.CodeMessage): Unit = coroutineScope {
            with(msg) {
                require(hasProgramDetail())
                logger.debug { "Receiving new program ${projectContext.toSimpleString()}" }

                val featureDescriptions =
                    FeatureDescriptionBuilder(CommonEntitySerde.toUUID(projectContext.projectId), programDetail)
                        .build()
                launch { datasetDao.saveFeatureDescription(featureDescriptions) }

                val dataMessage = DatasetEntities.DatasetMessage.newBuilder()
                    .setProjectContext(projectContext)
                    .setFeatureDescriptionsDetail(DatasetEntitySerde.to(featureDescriptions))
                    .build()

                launch { datasetProducer.sendAsync(dataMessage).await() }
            }
        }
    }

    inner class MonitoringSubscriber : MessageListener<MonitoringEntities.MonitoringMessage> {
        private val factories = mutableMapOf<CommonEntities.ProjectContext, DatapointFactory>()

        override fun received(
            consumer: Consumer<MonitoringEntities.MonitoringMessage>,
            msg: Message<MonitoringEntities.MonitoringMessage>
        ) = runBlocking {
            val monitoringMessage = msg.value
            when (monitoringMessage.payloadCase) {
                MonitoringEntities.MonitoringMessage.PayloadCase.FRAME_STREAM_DETAIL -> {
                    handleFrameMessageDetail(monitoringMessage)
                    consumer.acknowledgeAsync(msg).await()
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                    consumer.acknowledgeAsync(msg).await()
                }
            }
            return@runBlocking
        }

        private suspend fun handleFrameMessageDetail(msg: MonitoringEntities.MonitoringMessage) = coroutineScope {
            with(msg) {
                require(hasFrameStreamDetail())
                logger.debug { "Received ${frameStreamDetail.framesCount} frames @ ${projectContext.toSimpleString()} (${frameStreamDetail.control.type})" }

                val factory = when (frameStreamDetail.control.type) {
                    CommonEntities.ControlType.OPEN -> {
                        require(projectContext !in factories)
                        val featureDescriptions = datasetDao.loadFeatureDescriptions(projectContext)
                        DatapointFactory(CommonEntitySerde.toUUID(projectContext.sessionId), featureDescriptions)
                            .also { factories[projectContext] = it }
                    }
                    CommonEntities.ControlType.HEARTBEAT -> {
                        require(projectContext in factories)
                        factories[projectContext]!!
                    }
                    CommonEntities.ControlType.CLOSE -> {
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
                    frameStreamDetail.control.type == CommonEntities.ControlType.CLOSE
                ) {
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