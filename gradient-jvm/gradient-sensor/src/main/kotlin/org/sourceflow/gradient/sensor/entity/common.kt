package org.sourceflow.gradient.sensor.entity

import java.util.*

enum class NameComponentType {
    GROUP, ARTIFACT, VERSION, PACKAGE, TYPE, PROPERTY, EXECUTABLE, PARAMETER
}

data class NameComponent(
        val type: NameComponentType,
        val value: String
)


data class CanonicalName(
        val components: List<NameComponent>,
        val genericLess: CanonicalName? = null
) : List<NameComponent> by components {
    private val stringRepresentation: String by lazy {
        val builder = StringBuilder()
        components.forEach {
            if (it.type == NameComponentType.PARAMETER) {
                builder.append(" | ").append(it.value)
            } else {
                if (builder.isNotEmpty()) builder.append(".")
                builder.append(it.value)
            }
        }

        builder.toString()
    }

    override fun toString(): String {
        return stringRepresentation
    }
}

// FIXME remove obsolete
data class Project(
        val name: CanonicalName,
        val sessionId: UUID = UUID.randomUUID(),
        var projectId: UUID = UUID.randomUUID()
)

enum class DataTypeDescriptor {
    NONE,
    BOOLEAN, BOOLEANS,
    INTEGER, INTEGERS,
    LONG, LONGS,
    FLOAT, FLOATS,
    DOUBLE, DOUBLES,
    STRING, STRINGS,
    REFERENCE, REFERENCES
}