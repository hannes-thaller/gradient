package org.sourceflow.gradient.sensor.persistence

import org.sourceflow.gradient.code.CodeEntity
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.sensor.entity.CanonicalName


internal object GrpcSerde {
    fun convert(e: CanonicalName): CommonEntity.CanonicalName {
        return CommonEntity.CanonicalName.newBuilder()
                .addAllComponents(e.components.map { it.value })
                .addAllTypes(e.components.map { CommonEntity.NameComponentType.valueOf(it.type.name) })
                .build()
    }

    fun convert(e: org.sourceflow.gradient.sensor.entity.CodeElement): CodeEntity.CodeElement {
        val element = CodeEntity.CodeElement.newBuilder()
                .setId(e.id)
                .setName(convert(e.name))
                .setStatus(CodeEntity.ModelingUniverseStatus.valueOf(e.status.name))
        when (e) {
            is org.sourceflow.gradient.sensor.entity.Type -> element.setType(
                    CodeEntity.Type.newBuilder()
                            .addAllProperties(e.properties.map { it.id })
                            .addAllExecutables(e.executables.map { it.id })
            )
            is org.sourceflow.gradient.sensor.entity.Property -> element.setProperty(
                    CodeEntity.Property.newBuilder()
                            .setIsClassMember(e.isClassMember)
                            .setIsImmutable(e.isImmutable)
                            .setDataType(convert(e.dataType))
            )
            is org.sourceflow.gradient.sensor.entity.Executable -> element.setExecutable(
                    CodeEntity.Executable.newBuilder()
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
                    CodeEntity.Parameter.newBuilder()
                            .setIndex(e.index)
                            .setDataType(convert(e.dataType))
            )
        }

        return element.build()
    }

    fun convert(e: org.sourceflow.gradient.sensor.entity.DataType): CodeEntity.DataType {
        return CodeEntity.DataType.newBuilder()
                .setDataTypeDescriptor(CodeEntity.DataTypeDescriptor.valueOf(e.dataTypeDescriptor.name))
                .setName(convert(e.name))
                .build()
    }
}