package org.sourceflow.gradient.dataset.persistence

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.bson.Document
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.dataset.entity.Datapoint
import org.sourceflow.gradient.dataset.entity.DatasetHandle
import org.sourceflow.gradient.dataset.entity.FeatureDescription
import java.util.*

class DatasetDao(client: MongoClient) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val featureDescriptionCol = client.getDatabase("service")
            .getCollection("featureDescriptions")
    private val datapointCol = client.getDatabase("service")
            .getCollection("datapoints")
    private val datasetHandleCol = client.getDatabase("service")
            .getCollection("datasets")
    private val datasetCol = client.getDatabase("service")
            .getCollection("datasets")

    init {
        setupIndexes()
    }

    private fun setupIndexes() {
        logger.debug { "Creating indexes" }

        featureDescriptionCol.createIndex(Indexes.ascending("elementId"))
        featureDescriptionCol.createIndex(Indexes.ascending("projectId"))
        datapointCol.createIndex(Indexes.ascending("sessionId", "featureDescriptionId"))
        datasetHandleCol.createIndex(Indexes.ascending("sessionId", "featureDescriptionId"))
    }

    suspend fun reset() {
        logger.debug { "Resetting collections" }

        withContext(Dispatchers.IO) {
            setupIndexes()
        }
    }

    suspend fun saveFeatureDescription(featureDescriptions: List<FeatureDescription>) {
        logger.debug { "Saving ${featureDescriptions.size} feature descriptions" }

        val docs = featureDescriptions.map {
            MongoSerde.to(it)
        }
        if (docs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                featureDescriptionCol.insertMany(docs)
            }
        }
    }

    suspend fun loadFeatureDescriptions(projectContext: CommonEntity.ProjectContext): List<FeatureDescription> {
        logger.debug { "Loading feature descriptions ${projectContext.toSimpleString()}" }

        val query = Document()
                .append("_dtype", "FeatureDescription")
                .append("projectId", CommonEntitySerde.to(projectContext.projectId))

        return withContext(Dispatchers.IO) {
            featureDescriptionCol.find(query)
                    .map { MongoSerde.fromFeatureDescription(it) }
                    .toList()
        }
    }

    suspend fun saveDatapoints(datapoints: List<Datapoint>) {
        logger.debug { "Saving datapoints ${datapoints.size}" }

        val docs = datapoints.map { MongoSerde.to(it) }

        if (docs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                datapointCol.insertMany(docs)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun loadDatapoints(datapointIds: List<UUID>): List<Datapoint> {
        logger.debug { "Loading datapoints ${datapointIds.size}" }

        val query = Document("_id", Document("{'$'}in", datapointIds))

        return withContext(Dispatchers.IO) {
            datapointCol.find(query)
                    .map { MongoSerde.fromDatapoint(it) }
                    .toList()
        }
    }

    suspend fun loadDatapointIds(sessionId: UUID,
                                 featureDescription: FeatureDescription,
                                 maximumLoadedIds: Int): List<UUID> {
        logger.debug { "Loading datapoint ids sessionId=$sessionId, elementId=${featureDescription.elementId} (max=$maximumLoadedIds)" }

        val query = Document()
                .append("sessionId", sessionId)
                .append("featureDescriptionId", featureDescription.id)

        val projection = Document("_id", true)

        return withContext(Dispatchers.IO) {
            datapointCol.find(query)
                    .projection(projection)
                    .limit(maximumLoadedIds)
                    .toList()
                    .map { it["_id"] as UUID }
        }
    }

    suspend fun saveDatasetHandle(datasetHandle: DatasetHandle) {
        logger.debug { "Saving dataset handle sessionId=${datasetHandle.sessionId}, elementId=${datasetHandle.featureDescription.elementId}" }

        val document = MongoSerde.to(datasetHandle)

        if (document.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                datasetHandleCol.insertOne(document)
            }
        }
    }

    suspend fun loadDatasetHandle(datasetId: UUID): DatasetHandle? {
        logger.debug { "Loading dataset handle $datasetId" }

        val query = Document("_id", datasetId)

        return withContext(Dispatchers.IO) {
            datasetHandleCol.find(query)
                    .first()
                    ?.let { MongoSerde.fromDatasetHandle(it) }
        }
    }
}