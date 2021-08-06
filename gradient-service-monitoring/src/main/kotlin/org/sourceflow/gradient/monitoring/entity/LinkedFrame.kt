package org.sourceflow.gradient.monitoring.entity

import org.sourceflow.gradient.monitoring.entities.MonitoringEntities

data class LinkedFrame(
    val id: Long,
    val frameElement: Int,
    val events: MutableList<MonitoringEntities.MonitoringEvent> = mutableListOf(),
    val childFrames: MutableList<LinkedFrame> = mutableListOf()
) {
    fun getRelatedChildEvents(): List<MonitoringEntities.MonitoringEvent> {
        return childFrames.flatMap { frame ->
            frame.events.filter {
                it.type == MonitoringEntities.MonitoringEventType.RECEIVE ||
                        it.type == MonitoringEntities.MonitoringEventType.RETURN ||
                        it.type == MonitoringEntities.MonitoringEventType.EXCEPT
            }
        }
    }
}
