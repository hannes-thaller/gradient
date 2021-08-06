package org.sourceflow.gradient.dataset.persistence

import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.dataset.entities.DatasetEntities
import org.sourceflow.gradient.dataset.entity.*


object DatasetEntitySerde {
    fun to(featureDescriptions: List<FeatureDescription>): DatasetEntities.FeatureDescriptionDetail {
        return DatasetEntities.FeatureDescriptionDetail.newBuilder()
            .addAllFeatureDescriptions(featureDescriptions.map { to(it) })
            .build()
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

    fun from(e: CodeEntities.DataType): DataType {
        return DataType(
            DataTypeDescriptor.valueOf(e.dataTypeDescriptor.name),
            from(e.name)
        )
    }

    fun to(e: CanonicalName): CommonEntities.CanonicalName {
        return CommonEntities.CanonicalName.newBuilder()
            .addAllComponents(e.values.map { it.first })
            .addAllTypes(e.values.map { CommonEntities.NameComponentType.valueOf(it.second.name) })
            .build()
    }

    fun to(e: FeatureDescription): DatasetEntities.FeatureDescription {
        return DatasetEntities.FeatureDescription.newBuilder()
            .setElementId(e.elementId)
            .addAllFeatures(e.inputFeatures.map { to(it) })
            .setFeatureDescriptionId(CommonEntitySerde.fromUUID(e.id))
            .build()
    }

    fun to(e: Feature): DatasetEntities.Feature {
        return DatasetEntities.Feature.newBuilder()
            .setElementId(e.elementId)
            .setName(to(e.name))
            .setDataType(to(e.dataType))
            .setFeatureType(DatasetEntities.FeatureType.valueOf(e.featureType.name))
            .build()
    }

    fun to(e: DataType): CodeEntities.DataType {
        return CodeEntities.DataType.newBuilder()
            .setDataTypeDescriptor(CodeEntities.DataTypeDescriptor.valueOf(e.descriptor.name))
            .setName(to(e.name))
            .build()
    }

    fun from(e: DatasetHandle): DatasetEntities.DatasetHandleDetail {
        return DatasetEntities.DatasetHandleDetail.newBuilder()
            .setFeatureDescription(to(e.featureDescription))
            .setDatasetId(CommonEntitySerde.fromUUID(e.id))
            .build()
    }

    fun from(e: Datapoint): DatasetEntities.Datapoint {
        return DatasetEntities.Datapoint.newBuilder()
            .addAllDatum(e.data.map { CommonEntitySerde.fromReference(it) })
            .build()
    }
}