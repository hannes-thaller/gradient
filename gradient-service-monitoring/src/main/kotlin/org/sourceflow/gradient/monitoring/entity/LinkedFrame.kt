package org.sourceflow.gradient.monitoring.entity

import org.sourceflow.gradient.monitoring.MonitoringEntity

data class LinkedFrame(
        val id: Long,
        val frameElement: Int,
        val events: MutableList<MonitoringEntity.MonitoringEvent> = mutableListOf(),
        val childFrames: MutableList<LinkedFrame> = mutableListOf()) {
    fun getRelatedChildEvents(): List<MonitoringEntity.MonitoringEvent> {
        return childFrames.flatMap { frame ->
            frame.events.filter {
                it.type == MonitoringEntity.MonitoringEventType.RECEIVE ||
                        it.type == MonitoringEntity.MonitoringEventType.RETURN ||
                        it.type == MonitoringEntity.MonitoringEventType.EXCEPT
            }
        }
    }
}
