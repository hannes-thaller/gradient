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
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.monitoring.MonitoringEntity
import org.sourceflow.gradient.monitoring.MonitoringServiceGrpcKt
import java.util.*

class GrpcTest : StringSpec({
    val projectId = CommonEntitySerde.from(UUID.randomUUID())
    val sessionId = CommonEntitySerde.from(UUID.randomUUID())
    val projectContext = CommonEntity.ProjectContext.newBuilder()
            .setProjectId(projectId)
            .setSessionId(sessionId)
            .build()
    val request = MonitoringEntity.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(MonitoringEntity.MonitoringStreamDetail.getDefaultInstance())
            .build()
    val response = MonitoringEntity.MonitoringMessage.newBuilder()
            .setProjectContext(projectContext)
            .setMonitoringStreamDetail(MonitoringEntity.MonitoringStreamDetail.getDefaultInstance())
            .build()

    "should send message to server"{
        val result = mutableListOf<MonitoringEntity.MonitoringMessage>()
        val server = object : MonitoringServiceGrpcKt.MonitoringServiceCoroutineImplBase() {
            override suspend fun report(requests: Flow<MonitoringEntity.MonitoringMessage>): MonitoringEntity.MonitoringMessage {
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