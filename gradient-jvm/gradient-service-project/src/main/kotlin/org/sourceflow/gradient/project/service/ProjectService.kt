package org.sourceflow.gradient.project.service

import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.project.entities.ProjectEntities
import org.sourceflow.gradient.project.entity.Project
import org.sourceflow.gradient.project.persistence.ProjectDao
import org.sourceflow.gradient.project.persistence.ProtobufSerde
import org.sourceflow.gradient.project.services.ProjectServiceGrpcKt
import java.io.Closeable
import java.util.*

private val logger = KotlinLogging.logger {}

class ProjectService(
    client: PulsarClient,
    private val projectDao: ProjectDao
) : ProjectServiceGrpcKt.ProjectServiceCoroutineImplBase(), Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(ProjectEntities.ProjectMessage::class.java))
        .topic("project")
        .subscriptionName("gs-project-service")
        .messageListener(ProjectSubscriber())
        .subscribe()
    private val producer = client.newProducer(Schema.PROTOBUF(ProjectEntities.ProjectMessage::class.java))
        .topic("project")
        .create()

    override suspend fun registerProject(request: ProjectEntities.ProjectMessage): ProjectEntities.ProjectMessage {
        TODO()
    }

    inner class ProjectSubscriber : MessageListener<ProjectEntities.ProjectMessage> {
        override fun received(
            consumer: Consumer<ProjectEntities.ProjectMessage>,
            msg: Message<ProjectEntities.ProjectMessage>
        ): Unit = runBlocking<Unit> {
            val projectMessage = msg.value
            when {
                projectMessage.hasName() -> {
                    val answer = nameMessage(projectMessage)
                    producer.sendAsync(answer).await()
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                }
            }
            consumer.acknowledgeAsync(msg).await()
        }

        @OptIn(ExperimentalStdlibApi::class)
        private suspend fun nameMessage(msg: ProjectEntities.ProjectMessage) = with(msg) {
            assert(hasName())

            val projectContext = CommonEntities.ProjectContext.newBuilder()
                .setSessionId(requestId)

            val projectName = ProtobufSerde.from(name)
            val sessionId = CommonEntitySerde.toUUID(requestId)

            val project = projectDao.loadProjectByName(projectName)
            if (project != null) {
                logger.debug { "Registering session to project ${projectName.digest()}, session=${sessionId}" }

                val sessions = buildList {
                    addAll(project.sessions)
                    add(sessionId)
                }.distinct()

                projectContext.projectId = CommonEntitySerde.fromUUID(project.projectId)

                projectDao.setSessions(project.projectId, sessions)
            } else {
                logger.debug { "Creating new project ${projectName.digest()}, session=${sessionId}" }

                val projectId = UUID.randomUUID()
                projectContext.projectId = CommonEntitySerde.fromUUID(projectId)

                projectDao.saveProject(Project(projectName, projectId, listOf(sessionId)))
            }

            ProjectEntities.ProjectMessage.newBuilder()
                .setRequestId(requestId)
                .setContext(projectContext.build())
                .build()
        }
    }

    override fun close() {
        logger.debug { "Closing producers and consumers" }

        consumer.unsubscribe()
        consumer.close()
        producer.close()
    }
}