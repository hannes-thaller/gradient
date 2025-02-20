package org.sourceflow.gradient.dataset.entity

import com.fasterxml.uuid.Generators
import java.util.*

private val uuidGenerator = Generators.timeBasedGenerator()

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
}

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
) {
    fun isIterable(): Boolean {
        return descriptor.name.endsWith("S")
    }
}

enum class FeatureType {
    CONDITIONAL,
    INPUT_PROPERTY,
    INPUT_PARAMETER,
    INPUT_RESULT,
    OUTPUT_PROPERTY,
    OUTPUT_PARAMETER,
    OUTPUT_RESULT
}

data class Feature(
        val elementId: Int,
        val name: CanonicalName,
        val dataType: DataType,
        val featureType: FeatureType,
        val aliasIds: List<Int> = emptyList(),
        val id: UUID = uuidGenerator.generate()
)


data class FeatureDescription(
        val projectId: UUID,
        val elementId: Int,
        val name: CanonicalName,
        val inputFeatures: List<Feature>,
        val id: UUID = uuidGenerator.generate()
)

data class Datapoint(
        val sessionId: UUID,
        val featureDescriptionId: UUID,
        val data: List<*>,
        val id: UUID = uuidGenerator.generate()
)

data class DatasetHandle(
        val featureDescription: FeatureDescription,
        val datapointIds: List<UUID>,
        val sessionId: UUID,
        val id: UUID = UUID.randomUUID()
)