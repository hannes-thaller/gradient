package org.sourceflow.gradient.monitoring.persistence

import org.sourceflow.gradient.monitoring.MonitoringEntity
import org.sourceflow.gradient.monitoring.entity.LinkedFrame

object ProtobufSerde {
    fun to(frame: LinkedFrame): MonitoringEntity.Frame {
        return MonitoringEntity.Frame.newBuilder()
                .setId(frame.id)
                .setFrameCodeElementId(frame.frameElement)
                .addAllEvents(frame.events)
                .addAllRelatedChildEvents(frame.getRelatedChildEvents())
                .build()
    }
}