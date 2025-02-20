package org.sourceflow.gradient.code.persistence

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.UpdateOneModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.bson.Document
import org.sourceflow.gradient.code.entity.ModelingUniverseStatus
import org.sourceflow.gradient.code.entity.Program
import java.util.*

private val logger = KotlinLogging.logger { }

class CodeDao(client: MongoClient) {
    private val col = client.getDatabase("service")
            .getCollection("programs")

    init {
        setupIndex()
    }

    private fun setupIndex() {
        logger.debug { "Creating indexes" }

        col.createIndex(Indexes.ascending("_dtype"))
        col.createIndex(Indexes.ascending("elementId"))
        col.createIndex(Indexes.ascending("projectId"))
    }

    suspend fun reset() {
        logger.debug { "Resetting programs collection" }

        withContext(Dispatchers.IO) {
            col.drop()
            setupIndex()
        }
    }

    suspend fun saveProgram(program: Program): Program {
        val docs = program.map { MongoSerde.to(it) }

        logger.debug { "Saving program ${docs.size} documents" }
        if (docs.isNotEmpty()) {
            withContext(Dispatchers.IO) {
                col.insertMany(docs)
            }
        }

        return program
    }

    suspend fun loadStructuralRelatedCodeElements(projectId: UUID, modelElementIds: List<Int>): List<Pair<Int, ModelingUniverseStatus>> {
        val elementQuery = mapOf(
                "projectId" to projectId,
                "${'$'}or" to listOf(
                        mapOf("id" to mapOf("${'$'}in" to modelElementIds)),
                        mapOf("properties" to mapOf("${'$'}in" to modelElementIds)),
                        mapOf("executables" to mapOf("${'$'}in" to modelElementIds))
                )
        )

        val elementProjection = mapOf(
                "_id" to false,
                "id" to true,
                "status" to true
        )

        return withContext(Dispatchers.IO) {
            val result = col.find(Document(elementQuery))
                    .projection(Document(elementProjection))
                    .toList()
                    .map {
                        it["id"] as Int to ModelingUniverseStatus.valueOf(it["status"] as String)
                    }

            logger.debug { "Loaded ${result.size} related elements" }
            result
        }
    }

    suspend fun updateModelStatus(projectId: UUID, modelElementIds: List<Pair<Int, ModelingUniverseStatus>>) {
        val updates = modelElementIds.map {
            UpdateOneModel<Document>(
                    Document().append("projectId", projectId).append("id", it.first),
                    Document("${'$'}set", Document("status", it.second.name))
            )
        }

        if (updates.isNotEmpty()) {
            withContext<Unit>(Dispatchers.IO) {
                logger.debug { "Updating  ${updates.size} documents" }
                col.bulkWrite(updates)
            }
        }
    }
}