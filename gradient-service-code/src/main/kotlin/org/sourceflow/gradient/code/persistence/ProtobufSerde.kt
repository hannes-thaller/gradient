package org.sourceflow.gradient.code.persistence

import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.code.entity.*
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import java.util.*

object ProtobufSerde {
    fun from(e: CodeEntities.ProgramDetail, id: CommonEntities.UUID): Program {
        val projectId = CommonEntitySerde.toUUID(id)
        val properties = e.propertiesList.associate { it.id to fromProperty(it, projectId) }
        val parameters = e.parametersList.associate { it.id to convertParameter(it, projectId) }
        val executables = e.executablesList.associate { it.id to fromExecutable(it, projectId) }

        e.executablesList.map { el ->
            val executable = executables.getValue(el.id)
            el.executable.parametersList.mapTo(executable.parameters, parameters::getValue)
            el.executable.invokesList.mapTo(executable.invokes, executables::getValue)
            el.executable.readsList.mapTo(executable.reads, properties::getValue)
            el.executable.writesList.mapTo(executable.writes, properties::getValue)
        }

        val types = e.typesList.map { el ->
            val type = fromType(el, projectId)
            el.type.propertiesList.mapTo(type.properties, properties::getValue)
            el.type.executablesList.mapTo(type.executables) {
                executables.getValue(it)
            }

            type
        }

        return Program(
            projectId,
            types,
            properties.values.toList(),
            executables.values.toList(),
            parameters.values.toList()
        )
    }

    private fun fromType(e: CodeEntities.CodeElement, projectId: UUID): Type {
        require(e.hasType())
        return Type(
            projectId,
            e.id,
            from(e.name),
            ModelingUniverseStatus.valueOf(e.status.name)
        )
    }

    private fun fromProperty(e: CodeEntities.CodeElement, projectId: UUID): Property {
        require(e.hasProperty())

        val inner = e.property
        return Property(
            projectId,
            e.id,
            from(e.name),
            ModelingUniverseStatus.valueOf(e.status.name),
            inner.isClassMember,
            inner.isImmutable,
            from(inner.dataType)
        )
    }

    private fun fromExecutable(e: CodeEntities.CodeElement, projectId: UUID): Executable {
        require(e.hasExecutable())

        val inner = e.executable
        return Executable(
            projectId,
            e.id,
            from(e.name),
            ModelingUniverseStatus.valueOf(e.status.name),
            inner.isClassMember,
            inner.isAbstract,
            inner.isConstructor,
            dataType = from(inner.dataType)
        )
    }

    private fun convertParameter(e: CodeEntities.CodeElement, projectId: UUID): Parameter {
        require(e.hasParameter())

        val inner = e.parameter
        return Parameter(
            projectId,
            e.id,
            from(e.name),
            ModelingUniverseStatus.valueOf(e.status.name),
            inner.index,
            from(inner.dataType)
        )
    }

    fun from(e: CodeEntities.DataType): DataType {
        return DataType(
            DataTypeDescriptor.valueOf(e.dataTypeDescriptor.name),
            from(e.name)
        )
    }

    fun from(e: CommonEntities.CanonicalName): CanonicalName {
        return CanonicalName(
            e.componentsList
                .zip(e.typesList)
                .map { Pair(it.first, from(it.second)) }
        )
    }

    fun from(e: CommonEntities.NameComponentType): NameComponentType {
        return NameComponentType.valueOf(e.name)
    }
}