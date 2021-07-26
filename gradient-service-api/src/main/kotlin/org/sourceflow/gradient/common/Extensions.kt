package org.sourceflow.gradient.common

fun CommonEntity.ProjectContext.toSimpleString(): String {
    return "Project(projectId=${CommonEntitySerde.to(this.sessionId)}, sessionId=${CommonEntitySerde.to(this.sessionId)})"
}
