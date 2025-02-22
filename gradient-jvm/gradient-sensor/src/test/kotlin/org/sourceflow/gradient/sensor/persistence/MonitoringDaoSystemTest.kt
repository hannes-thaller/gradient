package org.sourceflow.gradient.sensor.persistence

import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import org.sourceflow.gradient.sensor.DIContainer
import org.sourceflow.gradient.sensor.monitoring.ByteCodeFacade
import java.io.File
import java.util.*
import kotlin.system.measureTimeMillis
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@OptIn(ExperimentalTime::class)
class MonitoringDaoSystemTest : StringSpec() {

    private fun createEvent(datum: Int, source: Int, target: Int, frame: Long): MonitoringEntities.MonitoringEvent {
        return MonitoringEntities.MonitoringEvent.newBuilder()
            .setDatum(CommonEntitySerde.fromInt(datum))
            .setSource(source)
            .setTarget(target)
            .setFrameId(frame)
            .build()
    }

    init {
        val report = File("report", "MonitoringDaoSystemTestKt.csv")
        if (!report.exists()) {
            report.appendText("Id, MessageCount, DurationInMillis\n")
        }

        report.createNewFile()

        val projectContext = CommonEntities.ProjectContext.newBuilder()
            .setProjectId(CommonEntitySerde.fromUUID(UUID.randomUUID()))
            .setSessionId(CommonEntitySerde.fromUUID(UUID.randomUUID()))
            .build()

        "should report no events".config(enabled = false) {
            val sut = DIContainer.monitoringDao

            forAll<Int, Int, Int, Long> { datum, source, target, frame ->
                val msg = createEvent(datum, source, target, frame)
                sut.reportEvent(msg)
                sut.getMessagesSend() shouldBe 0L
            }
        }

        "should report events".config(enabled = false) {
            val sut = DIContainer.monitoringDao

            val msgCount = 10_000_000
            val time = measureTimeMillis {
                sut.reportOn(projectContext)
                checkAll<Int, Int, Int>(msgCount) { datum, source, target ->
                    ByteCodeFacade.frame(source).let {
                        ByteCodeFacade.read(datum, source, target, it)
                        ByteCodeFacade.returnsV(source, it)
                    }
                }
                sut.close()
            }

            println("Duration: ${time.milliseconds}, Msg/s: ${time.milliseconds / msgCount}")
            sut.getMessagesSend() shouldBe 3 * msgCount

            report.appendText("03f0dcfc-a882-4e4f-88cb-6d6b482b19b6, $msgCount, $time\n")
        }
    }
}