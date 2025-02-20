package org.sourceflow.gradient.code.entity

import java.util.*


enum class NameComponentType {
    GROUP,
    ARTIFACT,
    VERSION,
    PACKAGE,
    TYPE,
    PROPERTY,
    EXECUTABLE,
    PARAMETER,
    RESULT
}

data class CanonicalName(val values: List<Pair<String, NameComponentType>>) {
    companion object {
        fun from(vararg args: Pair<String, NameComponentType>): CanonicalName {
            return CanonicalName(args.toList())
        }
    }

    fun components(): List<String> {
        return values.map { it.first }
    }
}

data class Project(
        val name: CanonicalName,
        val projectId: UUID,
        val sessionId: UUID)

enum class DataTypeDescriptor {
    NONE,
    BOOLEAN, BOOLEANS,
    INTEGER, INTEGERS,
    LONG, LONGS,
    FLOAT, FLOATS,
    DOUBLE, DOUBLES,
    STRING, STRINGS,
    REFERENCE, REFERENCES;
}

data class DataType(
        val descriptor: DataTypeDescriptor,
        val name: CanonicalName
)

enum class ModelingUniverseStatus {
    EXTERNAL, BOUNDARY, INTERNAL,
    BOUNDARY_MODEL, INTERNAL_MODEL
}

interface CodeElement {
    val id: Int
    val name: CanonicalName
    var status: ModelingUniverseStatus
}

data class Type(
        val projectId: UUID,
        override val id: Int,
        override val name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        val properties: MutableList<Property> = mutableListOf(),
        val executables: MutableList<Executable> = mutableListOf()
) : CodeElement

data class Property(
        val projectId: UUID,
        override val id: Int,
        override val name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        val isClassMember: Boolean,
        val isImmutable: Boolean,
        val dataType: DataType
) : CodeElement

data class Executable(
        val projectId: UUID,
        override val id: Int,
        override val name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        val isClassMember: Boolean,
        val isAbstract: Boolean,
        val isConstructor: Boolean,
        val parameters: MutableList<Parameter> = mutableListOf(),
        val invokes: MutableList<Executable> = mutableListOf(),
        val reads: MutableList<Property> = mutableListOf(),
        val writes: MutableList<Property> = mutableListOf(),
        val dataType: DataType
) : CodeElement

data class Parameter(
        val projectId: UUID,
        override val id: Int,
        override val name: CanonicalName,
        override var status: ModelingUniverseStatus = ModelingUniverseStatus.EXTERNAL,
        val index: Int = -1,
        val dataType: DataType
) : CodeElement

data class Program(
        val projectId: UUID,
        val types: List<Type>,
        val properties: List<Property>,
        val executables: List<Executable>,
        val parameters: List<Parameter>
) : Iterable<CodeElement> {
    override fun iterator(): Iterator<CodeElement> {
        return iterator {
            yieldAll(types)
            yieldAll(properties)
            yieldAll(executables)
            yieldAll(parameters)
        }
    }

}