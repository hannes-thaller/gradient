package org.sourceflow.gradient.common

import org.sourceflow.gradient.common.entities.ProjectContext

fun ProjectContext.toSimpleString(): String {
    return "Project(projectId=${CommonEntitySerde.toUUID(this.sessionId)}, sessionId=${CommonEntitySerde.toUUID(this.sessionId)})"
}
