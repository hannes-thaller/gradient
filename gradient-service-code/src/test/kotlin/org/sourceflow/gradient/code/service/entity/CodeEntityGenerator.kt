package org.sourceflow.gradient.code.service.entity

import io.kotest.property.Arb
import io.kotest.property.arbitrary.*
import org.sourceflow.gradient.code.entities.CodeEntities
import org.sourceflow.gradient.common.entities.CommonEntities

object CodeEntityGenerator {

    fun programs(): Arb<CodeEntities.ProgramDetail.Builder> {
        return arbitrary { rs ->
            var id = 0
            val elementCounts = Arb.int(1, 10)
            val statuses = modelUniverseStatuses()
            val names = canonicalNames()

            val parameterList = mutableListOf<CodeEntities.CodeElement.Builder>()
            val executableList = mutableListOf<CodeEntities.CodeElement.Builder>()
            val propertyList = mutableListOf<CodeEntities.CodeElement.Builder>()
            val typeList = types()
                .take(elementCounts.next(rs), rs)
                .map { type ->
                    properties().take(10, rs).mapTo(propertyList) { property ->
                        CodeEntities.CodeElement.newBuilder()
                            .setId(id++)
                            .setName(names.next(rs))
                            .setStatus(statuses.next(rs))
                            .setProperty(property)
                            .also { type.addProperties(it.id) }
                    }

                    executables().take(elementCounts.next(rs), rs).mapTo(executableList) { executable ->
                        parameters().take(elementCounts.next(rs), rs).mapTo(parameterList) { parameter ->
                            CodeEntities.CodeElement.newBuilder()
                                .setId(id++)
                                .setName(names.next(rs))
                                .setStatus(statuses.next(rs))
                                .setParameter(parameter)
                                .also { executable.addParameters(it.id) }
                        }

                        CodeEntities.CodeElement.newBuilder()
                            .setId(id++)
                            .setName(names.next(rs))
                            .setStatus(statuses.next(rs))
                            .setExecutable(executable)
                            .also { type.addExecutables(it.id) }
                    }

                    CodeEntities.CodeElement.newBuilder()
                        .setId(id++)
                        .setName(names.next(rs))
                        .setStatus(statuses.next(rs))
                        .setType(type)
                }
                .toList()

            val invokes = Arb.subsequence(executableList)
            val reads = Arb.subsequence(propertyList)
            val writes = Arb.subsequence(propertyList)
            executableList.forEach { executable ->
                executable.executableBuilder
                    .addAllInvokes(invokes.next(rs).map { it.id })
                    .addAllReads(reads.next(rs).map { it.id })
                    .addAllWrites(writes.next(rs).map { it.id })
            }

            CodeEntities.ProgramDetail.newBuilder()
                .addAllTypes(typeList.map { it.build() })
                .addAllProperties(propertyList.map { it.build() })
                .addAllExecutables(executableList.map { it.build() })
                .addAllParameters(parameterList.map { it.build() })
        }
    }

    fun types(): Arb<CodeEntities.Type.Builder> {
        return arbitrary {
            CodeEntities.Type.newBuilder()
        }
    }

    fun properties(): Arb<CodeEntities.Property.Builder> {
        return arbitrary { rs ->
            val arbBoolean = Arb.bool()

            CodeEntities.Property.newBuilder()
                .setIsClassMember(arbBoolean.next(rs))
                .setIsImmutable(arbBoolean.next(rs))
                .setDataType(dataTypes().next(rs))
        }
    }

    fun executables(): Arb<CodeEntities.Executable.Builder> {
        return arbitrary { rs ->
            val bools = Arb.bool()
            val dataTypes = dataTypes()
            CodeEntities.Executable.newBuilder()
                .setIsClassMember(bools.next(rs))
                .setIsAbstract(bools.next(rs))
                .setIsConstructor(bools.next(rs))
                .setDataType(dataTypes.next(rs))
        }
    }

    fun parameters(maxIndex: Int = Integer.MAX_VALUE): Arb<CodeEntities.Parameter.Builder> {
        return arbitrary { rs ->
            val positiveInts = Arb.positiveInts(maxIndex)
            CodeEntities.Parameter.newBuilder()
                .setIndex(positiveInts.next(rs))
        }
    }

    fun canonicalNames(length: Arb<Int> = Arb.int(1, 7)): Arb<CommonEntities.CanonicalName.Builder> {
        return arbitrary { rs ->
            val nameLength = length.next(rs)
            val strings = Arb.string(3, 10)
            val components = nameComponentTypes()

            CommonEntities.CanonicalName.newBuilder()
                .addAllComponents(strings.take(nameLength).toList())
                .addAllTypes(components.take(nameLength).toList())
        }
    }

    fun nameComponentTypes(): Arb<CommonEntities.NameComponentType> {
        return Arb.enum<CommonEntities.NameComponentType>()
            .filter { it != CommonEntities.NameComponentType.UNRECOGNIZED }
    }

    fun dataTypes(nameLength: Arb<Int> = Arb.int(1, 7)): Arb<CodeEntities.DataType.Builder> {
        return Arb.bind(canonicalNames(nameLength), dataTypeDescriptors()) { name, type ->
            CodeEntities.DataType.newBuilder()
                .setName(name)
                .setDataTypeDescriptor(type)
        }
    }

    fun modelUniverseStatuses(): Arb<CodeEntities.ModelingUniverseStatus> {
        return Arb.enum<CodeEntities.ModelingUniverseStatus>()
            .filter { it != CodeEntities.ModelingUniverseStatus.UNRECOGNIZED }
    }

    fun dataTypeDescriptors(): Arb<CodeEntities.DataTypeDescriptor> {
        return Arb.enum<CodeEntities.DataTypeDescriptor>()
            .filter { it != CodeEntities.DataTypeDescriptor.UNRECOGNIZED }
    }
}