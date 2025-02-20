package org.sourceflow.gradient.sensor.entity

import io.github.classgraph.ClassInfo
import io.github.classgraph.FieldInfo
import io.github.classgraph.MethodInfo
import io.github.classgraph.MethodParameterInfo


class CodeElementGraph {

    private val index = mutableMapOf<CanonicalName, Int>()

    val types = mutableListOf<Type>()
    private val typeInfos = mutableListOf<ClassInfo>()

    val properties = mutableListOf<Property>()
    private val propertyInfos = mutableListOf<FieldInfo>()

    val executables = mutableListOf<Executable>()
    private val executableInfos = mutableListOf<MethodInfo>()

    val parameters = mutableListOf<Parameter>()
    private val parameterInfos = mutableListOf<MethodParameterInfo>()

    fun codeElements(): Sequence<CodeElement> {
        return sequence {
            yieldAll(types)
            yieldAll(properties)
            yieldAll(executables)
            yieldAll(parameters)
        }
    }

    fun nameMap(): Map<CanonicalName, CodeElement> {
        val result = mutableMapOf<CanonicalName, CodeElement>()
        for (codeElement in codeElements()) {
            result[codeElement.name] = codeElement
            codeElement.name.genericLess?.let { result[it] = codeElement }
        }
        return result
    }

    fun getOrPut(ci: ClassInfo): Type {
        val name = CanonicalNameFactory.newTypeName(ci.name)

        return if (name in index) {
            types[index.getValue(name)]
        } else {
            Type(name = name).also {
                assert(types.size == typeInfos.size)
                types.add(it)
                typeInfos.add(ci)
                index[name] = types.size - 1
            }
        }
    }

    fun getOrPut(ci: ClassInfo, fi: FieldInfo): Property {
        val typeName = CanonicalNameFactory.newTypeName(ci.name)
        val name = CanonicalNameFactory.newPropertyName(typeName, fi.name)

        return if (name in index) {
            properties[index.getValue(name)]
        } else {
            val dataType = DataTypeFactory.fromDescriptor(fi.typeDescriptor)
            Property(
                name = name,
                isImmutable = fi.isFinal,
                isClassMember = fi.isStatic,
                dataType = dataType
            ).also {
                assert(properties.size == propertyInfos.size)
                properties.add(it)
                propertyInfos.add(fi)
                index[name] = properties.size - 1
            }
        }
    }

    fun getOrPut(ci: ClassInfo, mi: MethodInfo): Executable {
        val typeName = CanonicalNameFactory.newTypeName(ci.name)
        val name = CanonicalNameFactory.newMethodName(typeName, mi.name, mi.typeDescriptorStr)

        return if (name in index) {
            executables[index.getValue(name)]
        } else {
            val dataType = DataTypeFactory.fromDescriptor(mi.typeSignatureOrTypeDescriptor.resultType)
            Executable(
                name = name,
                isClassMember = mi.isStatic,
                isAbstract = mi.hasBody(),
                isConstructor = mi.isConstructor,
                dataType = dataType
            ).also {
                assert(executables.size == executableInfos.size)
                executables.add(it)
                executableInfos.add(mi)
                index[name] = executables.size - 1
            }
        }
    }

    fun getOrPut(ci: ClassInfo, mi: MethodInfo, mpi: MethodParameterInfo, parameterIndex: Int): Parameter {
        val typeName = CanonicalNameFactory.newTypeName(ci.name)
        val methodName = CanonicalNameFactory.newMethodName(typeName, mi.name, mi.typeDescriptorStr)
        val parameterName = CanonicalNameFactory.newParameterName(
            methodName, mpi.name
                ?: "param${parameterIndex}"
        )

        return if (parameterName in index) {
            parameters[index.getValue(parameterName)]
        } else {
            val dataType = DataTypeFactory.fromDescriptor(mpi.typeSignatureOrTypeDescriptor)
            Parameter(
                name = parameterName,
                index = parameterIndex,
                dataType = dataType
            ).also {
                assert(parameters.size == parameterInfos.size)
                parameters.add(it)
                parameterInfos.add(mpi)
                index[parameterName] = parameters.size - 1
            }
        }
    }

    fun typesAndInfo(): List<Pair<ClassInfo, Type>> {
        return typeInfos zip types
    }

    fun inUniverseTypeNames(): List<String> {
        return typesAndInfo()
            .filter { it.second.status == ModelingUniverseStatus.INTERNAL_MODEL }
            .map { it.first.name }
    }

    fun addCodeElementsToModelUniverse(ids: List<Int>) {
        ids.toSet().let { accepted ->
            codeElements().forEach {
                when (it.status) {
                    ModelingUniverseStatus.BOUNDARY -> {
                        if (it.id in accepted) {
                            it.status = ModelingUniverseStatus.BOUNDARY_MODEL
                        }
                    }
                    ModelingUniverseStatus.INTERNAL -> {
                        if (it.id in accepted) {
                            it.status = ModelingUniverseStatus.INTERNAL_MODEL
                        }
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun isNotEmpty(): Boolean {
        return index.isNotEmpty()
    }

    override fun toString(): String {
        return "CodeElementGraph(type=${types.size}, property=${properties.size}, executable=${executables.size} " +
                "parameter=${parameters.size})"
    }
}