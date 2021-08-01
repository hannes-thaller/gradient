package org.sourceflow.gradient.monitoring.persistence

import org.sourceflow.gradient.monitoring.entities.MonitoringEntities
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

object ProtobufSerde {
    fun to(frame: LinkedFrame): MonitoringEntities.Frame {
        return MonitoringEntities.Frame.newBuilder()
            .setId(frame.id)
            .setFrameCodeElementId(frame.frameElement)
            .addAllEvents(frame.events)
            .addAllRelatedChildEvents(frame.getRelatedChildEvents())
            .build()
    }
}