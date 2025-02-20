package org.sourceflow.gradient.sensor.persistence

import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import org.sourceflow.gradient.monitoring.services.MonitoringServiceGrpcKt
import java.util.*

class GrpcTest : StringSpec({
    val projectId = CommonEntitySerde.fromUUID(UUID.randomUUID())
    val sessionId = CommonEntitySerde.fromUUID(UUID.randomUUID())
    val projectContext = CommonEntities.ProjectContext.newBuilder()
        .setProjectId(projectId)
        .setSessionId(sessionId)
        .build()
    val request = MonitoringEntities.MonitoringMessage.newBuilder()
        .setProjectContext(projectContext)
        .setMonitoringStreamDetail(MonitoringEntities.MonitoringStreamDetail.getDefaultInstance())
        .build()
    val response = MonitoringEntities.MonitoringMessage.newBuilder()
        .setProjectContext(projectContext)
        .setMonitoringStreamDetail(MonitoringEntities.MonitoringStreamDetail.getDefaultInstance())
        .build()

    "should send message to server"{
        val result = mutableListOf<MonitoringEntities.MonitoringMessage>()
        val server = object : MonitoringServiceGrpcKt.MonitoringServiceCoroutineImplBase() {
            override suspend fun report(requests: Flow<MonitoringEntities.MonitoringMessage>): MonitoringEntities.MonitoringMessage {
                requests.collect {
                    result.add(it)
                }
                return response
            }
        }.let {
            ServerBuilder.forPort(50000)
                .addService(it)
                .build()
        }
        withContext(Dispatchers.IO) { server.start() }

        val channel = ManagedChannelBuilder
            .forAddress("localhost", 50000)
            .usePlaintext()
            .build()
        val service = MonitoringServiceGrpcKt.MonitoringServiceCoroutineStub(channel)

        val resultResponse = service.report(flow {
            emit(request)
        })

        result shouldHaveSize 1
        result.first().hasMonitoringStreamDetail().shouldBeTrue()
        resultResponse.hasMonitoringStreamDetail().shouldBeTrue()

        channel.shutdown()
        server.shutdown()
    }
})