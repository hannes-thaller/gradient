package org.sourceflow.gradient.monitoring.service

import mu.KotlinLogging
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities.MonitoringEvent
import org.sourceflow.gradient.monitoring.entities.MonitoringEntities.MonitoringEventType
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

class FrameAccumulator {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val frames = mutableMapOf<Long, LinkedFrame>()
    var closedFrames = 0L

    fun isNotEmpty(): Boolean {
        return frames.isNotEmpty()
    }

    fun openFramesCount(): Int {
        return frames.size
    }

    fun getOpenFrames(): Map<Long, LinkedFrame> {
        return frames
    }

    fun accumulate(events: List<MonitoringEvent>): List<LinkedFrame> {
        val result = events.mapNotNull { event: MonitoringEvent ->
            var result: LinkedFrame? = null
            when (event.type) {
                MonitoringEventType.FRAME -> openFrame(event)
                MonitoringEventType.RECEIVE,
                MonitoringEventType.READ,
                MonitoringEventType.WRITE -> addToFrame(event)
                MonitoringEventType.RETURN,
                MonitoringEventType.EXCEPT -> result = closeFrame(event)
                else -> {
                }
            }

            result
        }
        closedFrames += result.size
        return result
    }

    private fun openFrame(event: MonitoringEvent) {
        val frame = LinkedFrame(event.frameId, event.target, mutableListOf(event))
        frames[frame.id] = frame

        if (event.hasDatum() && event.datum.datumSelectionCase == CommonEntities.Datum.DatumSelectionCase.LONG_DATUM) {
            val parentFrameId = event.datum.longDatum
            frames[parentFrameId]!!.childFrames.add(frame)
        }
    }

    private fun closeFrame(event: MonitoringEvent): LinkedFrame? {
        var frame: LinkedFrame? = null
        if (event.frameId in frames) {
            if (event.target > 0) { // boundary return
                frames.getValue(event.frameId).events.add(event)
            } else {
                frame = frames.remove(event.frameId)!!
                frame.events.add(event)
            }
        } else {
            logger.warn { "Dangling event encountered: frameId=${event.frameId}, source=${event.source}" }
        }

        return frame
    }

    private fun addToFrame(event: MonitoringEvent) {
        if (event.frameId in frames) {
            frames.getValue(event.frameId).events.add(event)
        } else {
            logger.warn { "Dangling event encountered: frameId=${event.frameId}, source=${event.source}" }
        }
    }
}
