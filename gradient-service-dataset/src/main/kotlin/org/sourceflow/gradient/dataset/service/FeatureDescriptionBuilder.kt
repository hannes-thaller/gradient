package org.sourceflow.gradient.dataset.service

import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.Generators
import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.entities.CommonEntities
import org.sourceflow.gradient.dataset.entity.*
import org.sourceflow.gradient.dataset.persistence.DatasetEntitySerde
import org.sourceflow.gradient.dataset.persistence.ProgramMessageDao
import java.util.*

class FeatureDescriptionBuilder(
    private val projectId: UUID,
    program: CodeEntities.ProgramDetail
) {
    private val programDao = ProgramMessageDao(program)

    companion object {
        private val uuidGenerator = Generators.timeBasedGenerator(EthernetAddress.fromInterface())
        private val validTypes = CodeEntities.DataTypeDescriptor.values()
            .filterNot {
                it == CodeEntities.DataTypeDescriptor.REFERENCE ||
                        it == CodeEntities.DataTypeDescriptor.REFERENCES ||
                        it == CodeEntities.DataTypeDescriptor.NONE
            }

        private val conditionalName = CanonicalName.from("__conditional" to NameComponentType.TYPE)
        private val intDataType = DataType(
            DataTypeDescriptor.INTEGER,
            CanonicalName.from(
                "java" to NameComponentType.PACKAGE,
                "lang" to NameComponentType.PACKAGE,
                "Integer" to NameComponentType.TYPE
            )
        )
        private val nameBlacklist = setOf(
            CanonicalName.from(
                "java" to NameComponentType.PACKAGE,
                "lang" to NameComponentType.PACKAGE,
                "Class" to NameComponentType.TYPE,
                "desiredAssertionStatus(): java.lang.Boolean" to NameComponentType.EXECUTABLE
            )
        )
    }

    fun build(): List<FeatureDescription> {
        return programDao.getExecutables()
            .filter(::isValidExecutable)
            .map { createFeatureDescription(it) }
            .filter(::isValidFeatureDescription)
            .map { it.copy(id = uuidGenerator.generate()) }
    }

    private fun createFeatureDescription(executable: CodeEntities.CodeElement): FeatureDescription {
        assert(executable.hasExecutable())

        val rawFeatures = mutableListOf<Feature>()

        with(rawFeatures) {
            add(Feature(-1, conditionalName, intDataType, FeatureType.CONDITIONAL))

            addAll(receiveFeatures(executable))
            addAll(resultFeatures(executable))

            addAll(readFeatures(executable))
            addAll(writeFeatures(executable))

            addAll(inUniverseInvokeResultFeatures(executable))
            addAll(inUniverseInvokeReceiveFeatures(executable))

            addAll(outUniverseInvokeReceiveFeatures(executable))
            addAll(outUniverseInvokeReturnFeatures(executable))
        }

        val features = filterAndSortFeatures(rawFeatures)
        return FeatureDescription(projectId, executable.id, DatasetEntitySerde.from(executable.name), features)
    }

    private fun filterAndSortFeatures(features: List<Feature>): List<Feature> {
        return features
            .filterNot { it.name in nameBlacklist }
            .sortedWith(
                compareBy(
                    { it.featureType.ordinal },
                    { it.dataType.descriptor.ordinal },
                    { it.name.toString() }
                )
            )
    }

    private fun receiveFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getParameters(executable)
            .filter { isModelable(it.parameter.dataType) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.parameter.dataType),
                    FeatureType.INPUT_PARAMETER
                )
            }
    }

    private fun readFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getPropertyReads(executable)
            .filter { isModelable(it.property.dataType) && !isConstant(it) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.property.dataType),
                    FeatureType.INPUT_PROPERTY
                )
            }
    }

    private fun writeFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getPropertyWrites(executable)
            .filter { isModelable(it.property.dataType) && !isConstant(it) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.property.dataType),
                    FeatureType.OUTPUT_PROPERTY
                )
            }
    }

    private fun resultFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        val result = mutableListOf<Feature>()
        if (isModelable(executable.executable.dataType)) {
            result.add(
                Feature(
                    executable.id,
                    DatasetEntitySerde.from(executable.name),
                    DatasetEntitySerde.from(executable.executable.dataType),
                    FeatureType.OUTPUT_RESULT
                )
            )
        }

        return result
    }

    private fun inUniverseInvokeResultFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getInvocations(executable)
            .filter { it.status == CodeEntities.ModelingUniverseStatus.INTERNAL && isModelable(it.executable.dataType) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.executable.dataType),
                    FeatureType.INPUT_RESULT
                )
            }
    }

    private fun inUniverseInvokeReceiveFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getInvocations(executable)
            .filter { it.status == CodeEntities.ModelingUniverseStatus.INTERNAL && !isModelable(it.executable.dataType) }
            .flatMap { programDao.getParameters(it) }
            .filter { isModelable(it.parameter.dataType) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.parameter.dataType),
                    FeatureType.OUTPUT_PARAMETER
                )
            }
    }

    private fun outUniverseInvokeReceiveFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getInvocations(executable)
            .filter { it.status == CodeEntities.ModelingUniverseStatus.BOUNDARY }
            .flatMap { programDao.getParameters(it) }
            .filter { isModelable(it.parameter.dataType) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.parameter.dataType),
                    FeatureType.OUTPUT_PARAMETER
                )
            }
    }

    private fun outUniverseInvokeReturnFeatures(executable: CodeEntities.CodeElement): List<Feature> {
        require(executable.hasExecutable())
        return programDao.getInvocations(executable)
            .filter { it.status == CodeEntities.ModelingUniverseStatus.BOUNDARY && isModelable(it.executable.dataType) }
            .map {
                Feature(
                    it.id,
                    DatasetEntitySerde.from(it.name),
                    DatasetEntitySerde.from(it.executable.dataType),
                    FeatureType.INPUT_RESULT
                )
            }
    }

    private fun isValidExecutable(executable: CodeEntities.CodeElement): Boolean {
        assert(executable.name.typesList.last() == CommonEntities.NameComponentType.EXECUTABLE)

        val name = executable.name.componentsList.last()
        return executable.status == CodeEntities.ModelingUniverseStatus.INTERNAL &&
                DatasetEntitySerde.from(executable.name) !in nameBlacklist &&
                !(name.length > 3 && name.startsWith("get")) &&
                !(name.length > 3 && name.startsWith("set"))
    }

    private fun isValidFeatureDescription(featureDescription: FeatureDescription): Boolean {
        val nonConditionalFeatures = featureDescription.inputFeatures
            .filterNot { it.featureType == FeatureType.CONDITIONAL }
        return featureDescription.inputFeatures.size > 1 &&
                !nonConditionalFeatures.all { it.featureType == FeatureType.INPUT_PARAMETER } &&
                !nonConditionalFeatures.all { it.featureType == FeatureType.OUTPUT_RESULT }
    }

    private fun isModelable(dataType: CodeEntities.DataType): Boolean {
        return dataType.dataTypeDescriptor in validTypes
    }

    private fun isConstant(property: CodeEntities.CodeElement): Boolean {
        property.hasProperty()
        return property.property.isImmutable and property.property.isClassMember
    }
}
