package org.sourceflow.gradient.dataset.persistence

import org.sourceflow.gradient.code.CodeEntity
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.common.CommonEntitySerde
import org.sourceflow.gradient.dataset.DatasetEntity
import org.sourceflow.gradient.dataset.entity.*


object DatasetEntitySerde {
    fun to(featureDescriptions: List<FeatureDescription>): DatasetEntity.FeatureDescriptionDetail {
        return DatasetEntity.FeatureDescriptionDetail.newBuilder()
                .addAllFeatureDescriptions(featureDescriptions.map { to(it) })
                .build()
    }

    fun from(e: CommonEntity.CanonicalName): CanonicalName {
        return CanonicalName(
                e.componentsList
                        .zip(e.typesList)
                        .map { Pair(it.first, from(it.second)) }
        )
    }

    fun from(e: CommonEntity.NameComponentType): NameComponentType {
        return NameComponentType.valueOf(e.name)
    }

    fun from(e: CodeEntity.DataType): DataType {
        return DataType(
                DataTypeDescriptor.valueOf(e.dataTypeDescriptor.name),
                from(e.name)
        )
    }

    fun to(e: CanonicalName): CommonEntity.CanonicalName {
        return CommonEntity.CanonicalName.newBuilder()
                .addAllComponents(e.values.map { it.first })
                .addAllTypes(e.values.map { CommonEntity.NameComponentType.valueOf(it.second.name) })
                .build()
    }

    fun to(e: FeatureDescription): DatasetEntity.FeatureDescription {
        return DatasetEntity.FeatureDescription.newBuilder()
                .setElementId(e.elementId)
                .addAllFeatures(e.inputFeatures.map { to(it) })
                .setFeatureDescriptionId(CommonEntitySerde.from(e.id))
                .build()
    }

    fun to(e: Feature): DatasetEntity.Feature {
        return DatasetEntity.Feature.newBuilder()
                .setElementId(e.elementId)
                .setName(to(e.name))
                .setDataType(to(e.dataType))
                .setFeatureType(DatasetEntity.FeatureType.valueOf(e.featureType.name))
                .build()
    }

    fun to(e: DataType): CodeEntity.DataType {
        return CodeEntity.DataType.newBuilder()
                .setDataTypeDescriptor(CodeEntity.DataTypeDescriptor.valueOf(e.descriptor.name))
                .setName(to(e.name))
                .build()
    }

    fun from(e: DatasetHandle): DatasetEntity.DatasetHandleDetail {
        return DatasetEntity.DatasetHandleDetail.newBuilder()
                .setFeatureDescription(to(e.featureDescription))
                .setDatasetId(CommonEntitySerde.from(e.id))
                .build()
    }

    fun from(e: Datapoint): DatasetEntity.Datapoint {
        return DatasetEntity.Datapoint.newBuilder()
                .addAllDatum(e.data.map { CommonEntitySerde.fromReference(it) })
                .build()
    }
}