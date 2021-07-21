package org.sourceflow.gradient.project.service

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.apache.pulsar.client.api.*
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.project.ProjectEntity
import org.sourceflow.gradient.project.ProjectServiceGrpcKt
import org.sourceflow.gradient.project.entity.Project
import org.sourceflow.gradient.project.persistence.ProjectDao
import org.sourceflow.gradient.project.persistence.ProtobufSerde
import org.sourceflow.pulsar.acknowledgeSuspend
import org.sourceflow.pulsar.sendSuspend
import java.io.Closeable
import java.util.*

private val logger = KotlinLogging.logger {}

class ProjectService(client: PulsarClient,
                     private val projectDao: ProjectDao) : ProjectServiceGrpcKt.ProjectServiceCoroutineImplBase(), Closeable {
    private val consumer = client.newConsumer(Schema.PROTOBUF(ProjectEntity.ProjectMessage::class.java))
            .topic("project")
            .subscriptionName("gs-project-service")
            .messageListener(ProjectSubscriber())
            .subscribe()
    private val producer = client.newProducer(Schema.PROTOBUF(ProjectEntity.ProjectMessage::class.java))
            .topic("project")
            .create()

    override suspend fun registerProject(request: ProjectEntity.ProjectMessage): ProjectEntity.ProjectMessage {
        TODO()
    }

    inner class ProjectSubscriber : MessageListener<ProjectEntity.ProjectMessage> {
        override fun received(consumer: Consumer<ProjectEntity.ProjectMessage>, msg: Message<ProjectEntity.ProjectMessage>): Unit = runBlocking<Unit> {
            val projectMessage = msg.value
            when {
                projectMessage.hasName() -> {
                    val answer = nameMessage(projectMessage)
                    producer.sendSuspend(answer)
                }
                else -> {
                    logger.debug { "Acknowledging irrelevant message" }
                }
            }
            consumer.acknowledgeSuspend(msg)
        }

        @OptIn(ExperimentalStdlibApi::class)
        private suspend fun nameMessage(msg: ProjectEntity.ProjectMessage) = with(msg) {
            assert(hasName())

            val projectContext = CommonEntity.ProjectContext.newBuilder()
                    .setSessionId(requestId)

            val projectName = ProtobufSerde.from(name)
            val sessionId = CommonEntitySerde.to(requestId)

            val project = projectDao.loadProjectByName(projectName)
            if (project != null) {
                logger.debug { "Registering session to project ${projectName.digest()}, session=${sessionId}" }

                val sessions = buildList {
                    addAll(project.sessions)
                    add(sessionId)
                }.distinct()

                projectContext.projectId = CommonEntitySerde.from(project.projectId)

                projectDao.setSessions(project.projectId, sessions)
            } else {
                logger.debug { "Creating new project ${projectName.digest()}, session=${sessionId}" }

                val projectId = UUID.randomUUID()
                projectContext.projectId = CommonEntitySerde.from(projectId)

                projectDao.saveProject(Project(projectName, projectId, listOf(sessionId)))
            }

            ProjectEntity.ProjectMessage.newBuilder()
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