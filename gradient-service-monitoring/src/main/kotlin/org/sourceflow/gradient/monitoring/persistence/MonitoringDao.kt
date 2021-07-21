package org.sourceflow.gradient.monitoring.persistence

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

class MonitoringDao(client: MongoClient) {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val col = client.getDatabase("service")
            .getCollection("frames")

    init {
        setupIndexes()
    }

    private fun setupIndexes() {
        logger.debug { "Creating indexes" }

        col.createIndex(Indexes.ascending("sessionId"))
        col.createIndex(Indexes.ascending("frameId"))
    }

    suspend fun reset() {
        withContext(Dispatchers.IO) {
            col.drop()
            setupIndexes()
        }
    }

    suspend fun saveFrames(projectContext: CommonEntity.ProjectContext, frames: List<LinkedFrame>) {
        logger.debug { "Saving ${frames.size} frames ${projectContext.toSimpleString()}" }

        val docs = frames.map {
            MongoSerde.to(it)
                    .append("projectContext", MongoSerde.to(projectContext))
        }

        if (docs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                col.insertMany(docs)
            }
        }
    }
}