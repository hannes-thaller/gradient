package org.sourceflow.gradient.sensor.entity

import java.util.concurrent.atomic.AtomicInteger

private val idGenerator = AtomicInteger(1)

enum class ModelingUniverseStatus {
    EXTERNAL, BOUNDARY, INTERNAL,
    BOUNDARY_MODEL, INTERNAL_MODEL
}

data class DataType(
        var dataTypeDescriptor: DataTypeDescriptor,
        var name: CanonicalName
)

interface CodeElement {
    var id: Int
    var name: CanonicalName
    var status: ModelingUniverseStatus
}

data class Type(
        override var id: Int = idGenerator.incrementAndGet(),
        override var name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        val properties: MutableList<Property> = mutableListOf(),
        val executables: MutableList<Executable> = mutableListOf()
) : CodeElement {
    override fun toString(): String {
        return name.toString()
    }
}

data class Property(
        override var id: Int = idGenerator.incrementAndGet(),
        override var name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        var isClassMember: Boolean = false,
        var isImmutable: Boolean = false,
        var dataType: DataType
) : CodeElement {
    override fun toString(): String {
        return name.toString()
    }
}

data class Executable(
        override var id: Int = idGenerator.incrementAndGet(),
        override var name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        var isClassMember: Boolean = false,
        var isAbstract: Boolean = false,
        var isConstructor: Boolean = false,
        val parameters: MutableList<Parameter> = mutableListOf(),
        val invokes: MutableList<Executable> = mutableListOf(),
        val reads: MutableList<Property> = mutableListOf(),
        val writes: MutableList<Property> = mutableListOf(),
        var dataType: DataType
) : CodeElement {
    override fun toString(): String {
        return name.toString()
    }
}

data class Parameter(
        override var id: Int = idGenerator.incrementAndGet(),
        override var name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        var index: Int = -1,
        var dataType: DataType
) : CodeElement {
    override fun toString(): String {
        return name.toString()
    }
}