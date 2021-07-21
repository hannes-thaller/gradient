package org.sourceflow.gradient.dataset.persistence

import org.bson.Document
import org.sourceflow.gradient.dataset.entity.*
import java.util.*

@Suppress("UNCHECKED_CAST")
object MongoSerde {
    fun to(e: FeatureDescription): Document {
        return Document()
                .append("_dtype", FeatureDescription::class.simpleName)
                .append("projectId", e.projectId)
                .append("elementId", e.elementId)
                .append("name", to(e.name))
                .append("inputFeatures", e.inputFeatures.map { to(it) })
                .append("featureDescriptionId", e.id)
    }

    fun fromFeatureDescription(e: Document): FeatureDescription {
        assert(e["_dtype"] == FeatureDescription::class.java.simpleName)
        return FeatureDescription(
                e["projectId"] as UUID,
                e["elementId"] as Int,
                fromCanonicalName(e["name"] as Document),
                (e["inputFeatures"] as List<Document>).map { fromFeature(it) },
                e["featureDescriptionId"] as UUID
        )
    }

    fun to(e: Feature): Document {
        return Document()
                .append("_dtype", Feature::class.simpleName)
                .append("elementId", e.elementId)
                .append("name", to(e.name))
                .append("dataType", to(e.dataType))
                .append("featureType", e.featureType.name)
                .append("aliasIds", e.aliasIds)
                .append("featureId", e.id)
    }

    fun fromFeature(e: Document): Feature {
        assert(e["_dtype"] == Feature::class.java.simpleName)
        return Feature(
                e["elementId"] as Int,
                fromCanonicalName(e["name"] as Document),
                fromDataType(e["dataType"] as Document),
                FeatureType.valueOf(e["featureType"] as String),
                e["aliasIds"] as List<Int>,
                e["featureId"] as UUID
        )
    }

    fun to(e: DataType): Document {
        return Document()
                .append("_dtype", DataType::class.simpleName)
                .append("descriptor", e.descriptor.name)
                .append("name", to(e.name))
    }

    fun fromDataType(e: Document): DataType {
        assert(e["_dtype"] == DataType::class.java.simpleName)
        return DataType(
                DataTypeDescriptor.valueOf(e["descriptor"] as String),
                fromCanonicalName(e["name"] as Document)
        )
    }

    fun to(e: CanonicalName): Document {
        return Document()
                .append("_dtype", CanonicalName::class.simpleName)
                .append("_digest", e.values.joinToString(".") { it.first })
                .append("components", e.values.map { it.first })
                .append("types", e.values.map { it.second.name })
    }

    fun fromCanonicalName(e: Document): CanonicalName {
        assert(e["_dtype"] == CanonicalName::class.java.simpleName)
        return CanonicalName(
                (e["components"] as List<String>)
                        .zip((e["types"] as List<String>)
                                .map { NameComponentType.valueOf(it) })
        )
    }

    fun to(e: Datapoint): Document {
        return Document()
                .append("_id", e.id)
                .append("sessionId", e.sessionId)
                .append("featureDescriptionId", e.featureDescriptionId)
                .append("data", e.data)
    }

    fun fromDatapoint(e: Document): Datapoint {
        return Datapoint(
                e["sessionId"] as UUID,
                e["featureDescriptionId"] as UUID,
                e["data"] as List<*>,
                e["_id"] as UUID
        )
    }

    fun to(e: DatasetHandle): Document {
        return Document()
                .append("_id", e.id)
                .append("sessionId", e.sessionId)
                .append("featureDescriptionId", e.featureDescription.id)
                .append("datapointIds", e.datapointIds)
    }

    fun fromDatasetHandle(e: Document): DatasetHandle {
        return DatasetHandle(
                fromFeatureDescription(e["featureDescription"] as Document),
                e["datapointIds"] as List<UUID>,
                e["sessionId"] as UUID,
                e["_id"] as UUID
        )
    }
}