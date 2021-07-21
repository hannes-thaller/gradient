package org.sourceflow.gradient.project.persistence

import com.mongodb.client.MongoClient
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import com.mongodb.client.model.Updates.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.sourceflow.gradient.project.entity.CanonicalName
import org.sourceflow.gradient.project.entity.Project
import java.util.*

private val logger = KotlinLogging.logger { }

class ProjectDao(client: MongoClient) {
    private val col = client.getDatabase("service")
            .getCollection("projects")

    init {
        setupIndex()
    }

    private fun setupIndex() {
        logger.debug { "Creating indexes" }

        val unique = IndexOptions().unique(true)
        col.createIndex(Indexes.ascending("projectId"), unique)
        col.createIndex(Indexes.ascending("name._digest"), unique)
    }

    suspend fun reset() {
        logger.debug { "Resetting projects collection" }

        withContext(Dispatchers.IO) {
            col.drop()
            setupIndex()
        }
    }

    suspend fun loadProjectByName(name: CanonicalName): Project? {
        logger.debug { "Loading project by name: ${name.digest()}" }

        val digest = name.values.joinToString(".") { it.first }

        return withContext(Dispatchers.IO) {
            col.find(eq("name._digest", digest))
                    .first()
                    ?.let { MongoSerde.fromProject(it) }
        }
    }

    suspend fun loadProjectBySession(sessionId: UUID): Project? {
        logger.debug { "Loading project by session: $sessionId" }

        return withContext(Dispatchers.IO) {
            col.find(eq("sessions", sessionId))
                    .first()
                    ?.let { MongoSerde.fromProject(it) }
        }
    }

    suspend fun saveProject(project: Project) {
        logger.debug { "Saving project: ${project.name.digest()}" }

        withContext(Dispatchers.IO) {
            col.insertOne(MongoSerde.to(project))
        }
    }

    suspend fun setSessions(projectId: UUID, sessions: List<UUID>) {
        logger.debug { "Adding session to project: projectId=${projectId}" }

        withContext(Dispatchers.IO) {
            col.updateOne(eq("projectId", projectId), set("sessions", sessions))
        }
    }
}