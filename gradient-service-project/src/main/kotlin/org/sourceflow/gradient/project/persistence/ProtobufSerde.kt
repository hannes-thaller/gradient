package org.sourceflow.gradient.project.persistence

import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.project.entity.CanonicalName
import org.sourceflow.gradient.project.entity.NameComponentType


object ProtobufSerde {
    fun from(e: CommonEntity.CanonicalName): CanonicalName {
        return CanonicalName(
                e.componentsList
                        .zip(e.typesList)
                        .map { Pair(it.first, from(it.second)) }
        )
    }

    fun from(e: CommonEntity.NameComponentType): NameComponentType {
        return NameComponentType.valueOf(e.name)
    }
}