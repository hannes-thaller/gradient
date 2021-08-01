package org.sourceflow.gradient.code.persistence

import org.bson.Document
import org.sourceflow.gradient.code.entity.*
import java.util.*

@Suppress("UNCHECKED_CAST")
object MongoSerde {
    fun to(e: CodeElement): Document {
        return when (e) {
            is Type -> to(e)
            is Property -> to(e)
            is Executable -> to(e)
            is Parameter -> to(e)
            else -> error("Unknown subtype of code element")
        }
    }

    fun to(e: Type): Document {
        return Document(
                mapOf(
                        "_dtype" to "Type",
                        "id" to e.id,
                        "projectId" to e.projectId,
                        "name" to to(e.name),
                        "status" to e.status.name,
                        "properties" to e.properties.map { it.id },
                        "executables" to e.executables.map { it.id }
                )
        )
    }

    fun to(e: Property): Document {
        return Document(
                mapOf<String, Any>(
                        "_dtype" to "Property",
                        "id" to e.id,
                        "projectId" to e.projectId,
                        "name" to to(e.name),
                        "status" to e.status.name,
                        "isClassMember" to e.isClassMember,
                        "isImmutable" to e.isImmutable,
                        "dataType" to to(e.dataType)
                )
        )
    }

    fun to(e: Executable): Document {
        return Document(
                mapOf<String, Any>(
                        "_dtype" to "Executable",
                        "id" to e.id,
                        "projectId" to e.projectId,
                        "name" to to(e.name),
                        "status" to e.status.name,
                        "isClassMember" to e.isClassMember,
                        "isAbstract" to e.isAbstract,
                        "isConstructor" to e.isConstructor,
                        "parameters" to e.parameters.map { it.id },
                        "invokes" to e.invokes.map { it.id },
                        "reads" to e.reads.map { it.id },
                        "writes" to e.writes.map { it.id },
                        "dataType" to to(e.dataType)
                )
        )
    }

    fun to(e: Parameter): Document {
        return Document(
                mapOf<String, Any>(
                        "_dtype" to "Parameter",
                        "id" to e.id,
                        "projectId" to e.projectId,
                        "name" to to(e.name),
                        "status" to e.status.name,
                        "index" to e.index,
                        "dataType" to to(e.dataType)
                )
        )
    }

    fun to(e: DataType): Document {
        return Document(
                mapOf<String, Any>(
                        "name" to to(e.name),
                        "descriptor" to e.descriptor.name
                )
        )
    }

    fun to(e: CanonicalName): Document {
        return Document()
                .append("_dtype", CanonicalName::class.simpleName)
                .append("_digest", e.values.joinToString(".") { it.first })
                .append("components", e.values.map { it.first })
                .append("types", e.values.map { it.second.name })
    }

    @Suppress("IMPLICIT_CAST_TO_ANY")
    fun <T> from(e: Document): T {
        require("_dtype" in e)
        val result = when (e["_dtype"]) {
            "Project" -> fromProject(e)
            "Type" -> fromType(e)
            "Property" -> fromProperty(e)
            "Executable" -> fromExecutable(e)
            "Parameter" -> fromParameter(e)
            else -> error("Unkown document type: ${e["_dtype"]}")
        }

        return result as T
    }

    private fun fromProject(e: Document): Project {
        require(e["_dtype"] == "Project")
        return Project(
                fromCanonicalName(e["name"] as Document),
                e["projectId"] as UUID,
                e["sessionId"] as UUID
        )
    }

    private fun fromType(e: Document): Type {
        require(e["_dtype"] == "Type")
        return Type(
                e["projectId"] as UUID,
                e["elementId"] as Int,
                fromCanonicalName(e["name"] as Document),
                ModelingUniverseStatus.valueOf(e["status"] as String)
        )
    }

    private fun fromProperty(e: Document): Property {
        require(e["_dtype"] == "Property")
        return Property(
                e["projectId"] as UUID,
                e["id"] as Int,
                fromCanonicalName(e["name"] as Document),
                ModelingUniverseStatus.valueOf(e["status"] as String),
                e["isClassMember"] as Boolean,
                e["isImmutable"] as Boolean,
                fromDataType(e["dataType"] as Document)
        )
    }

    private fun fromExecutable(e: Document): Executable {
        require(e["_dtype"] == "Executable")
        return Executable(
                e["projectId"] as UUID,
                e["id"] as Int,
                fromCanonicalName(e["name"] as Document),
                ModelingUniverseStatus.valueOf(e["status"] as String),
                e["isClassMember"] as Boolean,
                e["isAbstract"] as Boolean,
                e["isConstructor"] as Boolean,
                dataType = fromDataType(e["dataType"] as Document)
        )
    }

    private fun fromParameter(e: Document): Parameter {
        require(e["_dtype"] == "Parameter")
        return Parameter(
                e["projectId"] as UUID,
                e["id"] as Int,
                fromCanonicalName(e["name"] as Document),
                ModelingUniverseStatus.valueOf(e["status"] as String),
                e["index"] as Int,
                fromDataType(e["dataType"] as Document)
        )
    }

    fun fromCanonicalName(e: Document): CanonicalName {
        require(CanonicalName::class.simpleName == e["_dtype"])
        return CanonicalName(
                (e["components"] as List<String>)
                        .zip(e["types"] as List<String>)
                        .map { (l, r) -> l to NameComponentType.valueOf(r) }
        )
    }

    private fun fromDataType(e: Document): DataType {
        return DataType(
                DataTypeDescriptor.valueOf(e["descriptor"] as String),
                fromCanonicalName(e["name"] as Document)
        )
    }
}

