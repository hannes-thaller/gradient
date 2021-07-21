package org.sourceflow.gradient.project.persistence

import org.bson.Document
import org.sourceflow.gradient.project.entity.CanonicalName
import org.sourceflow.gradient.project.entity.NameComponentType
import org.sourceflow.gradient.project.entity.Project
import java.util.*

@Suppress("UNCHECKED_CAST")
object MongoSerde {
    fun <T> from(e: Document): T {
        require("_dtype" in e)
        val result = when (e["_dtype"]) {
            "Project" -> fromProject(e)
            else -> error("Unknown document type: ${e["_dtype"]}")
        }

        return result as T
    }

    fun fromCanonicalName(e: Document): CanonicalName {
        require(CanonicalName::class.simpleName == e["_dtype"])
        return CanonicalName(
                (e["components"] as List<String>)
                        .zip(e["types"] as List<String>)
                        .map { (l, r) -> l to NameComponentType.valueOf(r) }
        )
    }

    fun fromProject(e: Document): Project {
        require(Project::class.simpleName == e["_dtype"])
        return Project(
                name = fromCanonicalName(e["name"] as Document),
                projectId = e["projectId"] as UUID,
                sessions = e["sessions"] as List<UUID>
        )
    }

    fun to(e: Project): Document {
        return Document()
                .append("_dtype", Project::class.simpleName)
                .append("name", to(e.name))
                .append("projectId", e.projectId)
                .append("sessions", e.sessions)
    }

    fun to(e: CanonicalName): Document {
        return Document()
                .append("_dtype", CanonicalName::class.simpleName)
                .append("_digest", e.values.joinToString(".") { it.first })
                .append("components", e.values.map { it.first })
                .append("types", e.values.map { it.second.name })
    }
}