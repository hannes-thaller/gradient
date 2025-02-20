package org.sourceflow.gradient.code.service

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.code.entity.ModelingUniverseStatus
import org.sourceflow.gradient.code.persistence.CodeDao
import org.sourceflow.gradient.code.persistence.ProtobufSerde
import org.sourceflow.gradient.code.services.CodeServiceGrpcKt
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.dataset.entities.DatasetEntities
import java.io.Closeable


class CodeService(
    client: PulsarClient,
    private val codeDao: CodeDao
) : CodeServiceGrpcKt.CodeServiceCoroutineImplBase(), Closeable {
    private val codeProducer = client.newProducer(Schema.PROTOBUF(CodeEntities.CodeMessage::class.java))
        .topic("code")
        .create()
    private val codeConsumer = client.newConsumer(Schema.PROTOBUF(CodeEntities.CodeMessage::class.java))
        .topic("code")
        .subscriptionName("gs-code-service")
        .messageListener(CodeSubscriber())
        .subscribe()
    private val datasetConsumer = client.newConsumer(Schema.PROTOBUF(DatasetEntities.DatasetMessage::class.java))
        .topic("dataset")
        .subscriptionName("gs-code-service")
        .messageListener(DatasetSubscriber())
        .subscribe()

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override suspend fun analyzeProgram(request: CodeEntities.CodeMessage): CodeEntities.CodeMessage {
        TODO()
    }

    internal inner class DatasetSubscriber : MessageListener<DatasetEntities.DatasetMessage> {
        override fun received(
            consumer: Consumer<DatasetEntities.DatasetMessage>,
            msg: Message<DatasetEntities.DatasetMessage>
        ) = runBlocking {
            val datasetMessage = msg.value
            when (datasetMessage.payloadCase) {
                DatasetEntities.DatasetMessage.PayloadCase.FEATURE_DESCRIPTIONS_DETAIL -> {
                    handleFeatureDescriptionDetail(datasetMessage)
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                }
            }
            consumer.acknowledgeAsync(msg).await()
            return@runBlocking
        }

        /**
         * Type -> via properties and executables
         * Property -> direct via element id
         * Exectuable -> direct via element id
         * Parameters -> direct via element id
         */
        private suspend fun handleFeatureDescriptionDetail(msg: DatasetEntities.DatasetMessage) = with(msg) {
            assert(hasFeatureDescriptionsDetail())

            logger.debug { "Updating model elements ${CommonEntitySerde.toUUID(projectContext.projectId)}" }

            val projectId = CommonEntitySerde.toUUID(projectContext.projectId)
            val modelElementIds = extractModelElementIds(featureDescriptionsDetail.featureDescriptionsList)
            val elementsToPromote = codeDao.loadStructuralRelatedCodeElements(projectId, modelElementIds)
            val promotions = prepareModelPromotions(elementsToPromote)

            logger.debug { "Promoted ${promotions.size} elements into the modeling universe" }
            codeDao.updateModelStatus(projectId, promotions)

            val updateMsg = createUpdateMessage(projectContext, promotions)
            codeProducer.sendAsync(updateMsg).await()
        }

        @OptIn(ExperimentalStdlibApi::class)
        private fun extractModelElementIds(featureDescriptions: List<DatasetEntities.FeatureDescription>): List<Int> {
            return featureDescriptions
                .flatMap {
                    buildList {
                        addAll(it.featuresList.map { it.elementId })
                        addAll(it.featuresList.flatMap { it.aliasIdsList })
                    }
                }
                .distinct()
        }

        private fun prepareModelPromotions(elements: List<Pair<Int, ModelingUniverseStatus>>): List<Pair<Int, ModelingUniverseStatus>> {
            return elements
                .filter {
                    it.second == ModelingUniverseStatus.BOUNDARY ||
                            it.second == ModelingUniverseStatus.INTERNAL
                }
                .map {
                    val status = when (it.second) {
                        ModelingUniverseStatus.BOUNDARY -> {
                            ModelingUniverseStatus.BOUNDARY_MODEL
                        }
                        ModelingUniverseStatus.INTERNAL -> {
                            ModelingUniverseStatus.INTERNAL_MODEL
                        }
                        else -> {
                            error("Case should be filter by the previous stage")
                        }
                    }
                    it.copy(second = status)
                }
        }

        private fun createUpdateMessage(
            projectContext: CommonEntities.ProjectContext,
            updates: List<Pair<Int, ModelingUniverseStatus>>
        ): CodeEntities.CodeMessage {
            val updateDetail = CodeEntities.CodeElementModelUpdateDetail.newBuilder()
                .addAllUpdates(
                    updates.map {
                        CodeEntities.CodeElementModelUpdate.newBuilder()
                            .setElementId(it.first)
                            .setStatus(CodeEntities.ModelingUniverseStatus.valueOf(it.second.name))
                            .build()
                    }
                )

            return CodeEntities.CodeMessage.newBuilder()
                .setProjectContext(projectContext)
                .setModelUpdateDetail(updateDetail)
                .build()
        }
    }

    internal inner class CodeSubscriber : MessageListener<CodeEntities.CodeMessage> {
        override fun received(consumer: Consumer<CodeEntities.CodeMessage>, msg: Message<CodeEntities.CodeMessage>) =
            runBlocking {
                val codeMessage = msg.value
                when (codeMessage.payloadCase) {
                    CodeEntities.CodeMessage.PayloadCase.PROGRAM_DETAIL -> {
                        programMessage(codeMessage)
                    }
                    else -> {
                        logger.debug { "Acknowledging irrelevant message." }
                    }
                }
                consumer.acknowledgeAsync(msg).await()
                return@runBlocking
            }

        private suspend fun programMessage(msg: CodeEntities.CodeMessage) = with(msg) {
            assert(hasProgramDetail())

            logger.debug {
                "Receiving new program projectId=${CommonEntitySerde.to(projectContext.projectId)}, " +
                        "sessionId=${CommonEntitySerde.to(projectContext.sessionId)}"
            }

            codeDao.saveProgram(ProtobufSerde.from(programDetail, projectContext.projectId))

            logger.debug {
                "Saved types=${programDetail.typesCount}, " +
                        "properties=${programDetail.propertiesCount}, " +
                        "executables=${programDetail.executablesCount}, " +
                        "parameters=${programDetail.parametersCount}"
            }
        }
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        codeConsumer.unsubscribe()
        datasetConsumer.unsubscribe()
        codeConsumer.close()
        datasetConsumer.close()
        codeProducer.close()
    }
}