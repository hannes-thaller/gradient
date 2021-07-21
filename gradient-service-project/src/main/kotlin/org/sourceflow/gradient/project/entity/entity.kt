package org.sourceflow.gradient.project.entity

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

    fun digest(): String {
        return values.joinToString(".") { it.first }
    }
}

data class Project(
        val name: CanonicalName,
        val projectId: UUID,
        val sessions: List<UUID>)



