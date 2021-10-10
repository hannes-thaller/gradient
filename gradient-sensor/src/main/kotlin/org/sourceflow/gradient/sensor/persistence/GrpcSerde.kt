package org.sourceflow.gradient.sensor.persistence

import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.sensor.entity.CanonicalName


internal object GrpcSerde {
    fun convert(e: CanonicalName): CommonEntities.CanonicalName {
        return CommonEntities.CanonicalName.newBuilder()
                .addAllComponents(e.components.map { it.value })
                .addAllTypes(e.components.map { CommonEntities.NameComponentType.valueOf(it.type.name) })
                .build()
    }

    fun convert(e: org.sourceflow.gradient.sensor.entity.CodeElement): CodeEntities.CodeElement {
        val element = CodeEntities.CodeElement.newBuilder()
                .setId(e.id)
                .setName(convert(e.name))
                .setStatus(CodeEntities.ModelingUniverseStatus.valueOf(e.status.name))
        when (e) {
            is org.sourceflow.gradient.sensor.entity.Type -> element.setType(
                    CodeEntities.Type.newBuilder()
                            .addAllProperties(e.properties.map { it.id })
                            .addAllExecutables(e.executables.map { it.id })
            )
            is org.sourceflow.gradient.sensor.entity.Property -> element.setProperty(
                    CodeEntities.Property.newBuilder()
                            .setIsClassMember(e.isClassMember)
                            .setIsImmutable(e.isImmutable)
                            .setDataType(convert(e.dataType))
            )
            is org.sourceflow.gradient.sensor.entity.Executable -> element.setExecutable(
                    CodeEntities.Executable.newBuilder()
                            .setIsClassMember(e.isClassMember)
                            .setIsConstructor(e.isConstructor)
                            .setIsAbstract(e.isAbstract)
                            .addAllParameters(e.parameters.map { it.id })
                            .addAllInvokes(e.invokes.map { it.id })
                            .addAllReads(e.reads.map { it.id })
                            .addAllWrites(e.writes.map { it.id })
                            .setDataType(convert(e.dataType))
            )
            is org.sourceflow.gradient.sensor.entity.Parameter -> element.setParameter(
                    CodeEntities.Parameter.newBuilder()
                            .setIndex(e.index)
                            .setDataType(convert(e.dataType))
            )
        }

        return element.build()
    }

    fun convert(e: org.sourceflow.gradient.sensor.entity.DataType): CodeEntities.DataType {
        return CodeEntities.DataType.newBuilder()
                .setDataTypeDescriptor(CodeEntities.DataTypeDescriptor.valueOf(e.dataTypeDescriptor.name))
                .setName(convert(e.name))
                .build()
    }
}