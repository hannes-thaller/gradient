package org.sourceflow.gradient.sensor.entity

import io.github.classgraph.*
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.signature.SignatureReader
import org.objectweb.asm.signature.SignatureVisitor


internal object CanonicalNameFactory {
    val voidName = newTypeName("java.lang.Void")
    val booleanName = newTypeName("java.lang.Boolean")
    val byteName = newTypeName("java.lang.Byte")
    val charName = newTypeName("java.lang.Char")
    val shortName = newTypeName("java.lang.Short")
    val integerName = newTypeName("java.lang.Integer")
    val longName = newTypeName("java.lang.Long")
    val floatName = newTypeName("java.lang.Float")
    val doubleName = newTypeName("java.lang.Double")
    val stringName = newTypeName("java.lang.String")

    private val typeNames: Map<String, String> = mapOf(
        "Z" to "java.lang.Boolean",
        "C" to "java.lang.Character",
        "B" to "java.lang.Byte",
        "S" to "java.lang.Short",
        "I" to "java.lang.Integer",
        "J" to "java.lang.Long",
        "F" to "java.lang.Float",
        "D" to "java.lang.Double",
        "V" to "java.lang.Void",
        "java/lang/Boolean" to "java.lang.Boolean",
        "java/lang/Character" to "java.lang.Character",
        "java/lang/Byte" to "java.lang.Byte",
        "java/lang/Short" to "java.lang.Short",
        "java/lang/Integer" to "java.lang.Integer",
        "java/lang/Long" to "java.lang.Long",
        "java/lang/Float" to "java.lang.Float",
        "java/lang/Double" to "java.lang.Double",
        "java/lang/Void" to "java.lang.Void"
    )

    fun newProjectName(group: String, artifact: String, version: String): CanonicalName {
        require(group.isNotBlank())
        require(artifact.isNotBlank())
        require(version.isNotBlank())

        val groupComponents = group.split(".")
            .map {
                NameComponent(
                        NameComponentType.GROUP,
                        it
                )
            }
        val artifactComponent = artifact.split(".")
            .map {
                NameComponent(
                        NameComponentType.ARTIFACT,
                        it
                )
            }
        val versionComponent = version.split(".")
            .map {
                NameComponent(
                        NameComponentType.VERSION,
                        it
                )
            }

        return CanonicalName(groupComponents + artifactComponent + versionComponent)
    }

    fun newTypeName(name: String): CanonicalName {
        require(name.isNotBlank())
        return CanonicalName(typeNameComponents(name))
    }

    fun newPropertyName(typeName: CanonicalName, propertyName: String): CanonicalName {
        require(propertyName.isNotBlank())
        val components = typeName.components.toMutableList()
        components.add(NameComponent(NameComponentType.PROPERTY, propertyName.dropWhile { it == '$' }))

        return CanonicalName(components)
    }

    fun newMethodName(
            typeName: CanonicalName,
            methodName: String,
            descriptor: String
    ): CanonicalName {
        require(descriptor.isNotBlank())

        val typeComponents = typeName.components.toMutableList()
        val (parameterDescriptors, returnDescriptors) = methodNameComponents(descriptor)

        typeComponents.add(
                NameComponent(
                        NameComponentType.EXECUTABLE,
                        "$methodName(${parameterDescriptors.joinToString(", ")}): $returnDescriptors"
                )
        )
        return CanonicalName(typeComponents)
    }

    fun newParameterName(methodName: CanonicalName, name: String): CanonicalName {
        require(name.isNotBlank())

        return CanonicalName(
                methodName.components + NameComponent(
                        NameComponentType.PARAMETER,
                        name
                )
        )
    }

    private fun typeNameComponents(name: String): MutableList<NameComponent> {
        val result = mutableListOf<NameComponent>()
        for (component in name.split(".")) {
            if (component.first().isUpperCase()) {
                for (subClass in component.split("$")) {
                    result.add(NameComponent(NameComponentType.TYPE, subClass))
                }
            } else {
                result.add(NameComponent(NameComponentType.PACKAGE, component))
            }
        }
        return result
    }

    private fun methodNameComponents(descriptor: String, signature: String? = null): Pair<MutableList<String>, String> {
        val parameterDescriptors = mutableListOf<String>()
        var returnDescriptor = "V"
        if (signature != null) {
            SignatureReader(signature).accept(object : SignatureVisitor(Opcodes.ASM7) {
                override fun visitParameterType(): SignatureVisitor {
                    return object : SignatureVisitor(Opcodes.ASM7) {
                        override fun visitClassType(name: String) {
                            parameterDescriptors.add(name)
                        }

                        override fun visitBaseType(descriptor: Char) {
                            parameterDescriptors.add(descriptor.toString())
                        }
                    }
                }

                override fun visitReturnType(): SignatureVisitor {
                    return object : SignatureVisitor(Opcodes.ASM7) {
                        override fun visitClassType(name: String) {
                            returnDescriptor = name
                        }

                        override fun visitBaseType(descriptor: Char) {
                            returnDescriptor = descriptor.toString()
                        }
                    }
                }
            })
        } else {
            org.objectweb.asm.Type.getArgumentTypes(descriptor).mapTo(parameterDescriptors) { toName(it) }
            returnDescriptor = toName(Type.getReturnType(descriptor))
        }

        return Pair(parameterDescriptors, returnDescriptor)
    }

    private fun toName(type: org.objectweb.asm.Type): String {
        return toName(type.internalName)
    }

    private fun toName(internalName: String): String {
        val arrayDimensions = internalName.takeWhile { it == '[' }.length
        val rawName = internalName.substring(arrayDimensions)
        val name = typeNames.getOrElse(rawName[0].toString()) {
            rawName
                .dropWhile { it == 'L' }
                .takeWhile { it != ';' }
                .replace("/", ".")
        }
        return name + "*".repeat(arrayDimensions)
    }
}

internal object DataTypeFactory {
    fun fromDescriptor(t: TypeSignature): DataType {
        return when (t) {
            is BaseTypeSignature -> resolve(t, true)
            is ClassRefTypeSignature -> {
                val isSingular = t.classInfo == null || !isCollection(t.classInfo)
                resolve(t, isSingular)
            }
            is ArrayTypeSignature -> {
                when (val elementSignature = t.elementTypeSignature) {
                    is BaseTypeSignature -> resolve(elementSignature, false)
                    is ClassRefTypeSignature -> resolve(elementSignature, false)
                    else -> error("Could not parse the type signature")
                }
            }
            is TypeVariableSignature -> fromDescriptor(t.resolve().classBound)
            else -> error("Could not parse the type signature")
        }
    }

    private fun isCollection(ci: ClassInfo): Boolean {
        return ci.implementsInterface("java.lang.Iterable") ||
                ci.implementsInterface("java.util.Collection")
    }

    private fun resolve(e: BaseTypeSignature, isSingular: Boolean): DataType {
        return when (e.typeSignatureChar.toString()) {
            "Z" -> DataType(
                    if (isSingular) DataTypeDescriptor.BOOLEAN else DataTypeDescriptor.BOOLEANS,
                    CanonicalNameFactory.booleanName
            )
            "C" -> DataType(
                    if (isSingular) DataTypeDescriptor.STRING else DataTypeDescriptor.STRINGS,
                    CanonicalNameFactory.stringName
            )
            "B" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "S" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "I" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "J" -> DataType(
                    if (isSingular) DataTypeDescriptor.LONG else DataTypeDescriptor.LONGS,
                    CanonicalNameFactory.longName
            )
            "F" -> DataType(
                    if (isSingular) DataTypeDescriptor.FLOAT else DataTypeDescriptor.FLOATS,
                    CanonicalNameFactory.floatName
            )
            "D" -> DataType(
                    if (isSingular) DataTypeDescriptor.DOUBLE else DataTypeDescriptor.DOUBLES,
                    CanonicalNameFactory.doubleName
            )
            "V" -> DataType(DataTypeDescriptor.NONE, CanonicalNameFactory.voidName)
            else -> error("Only primitive types allowed")
        }
    }

    private fun resolve(e: ClassRefTypeSignature, isSingular: Boolean): DataType {
        val type = if(e.typeArguments.size == 1){
            e.typeArguments.first().toString()
        }else{
            e.baseClassName
        }
        return when (type) {
            "java.lang.Boolean" -> DataType(
                    if (isSingular) DataTypeDescriptor.BOOLEAN else DataTypeDescriptor.BOOLEANS,
                    CanonicalNameFactory.booleanName
            )
            "java.lang.Character" -> DataType(
                    if (isSingular) DataTypeDescriptor.STRING else DataTypeDescriptor.STRINGS,
                    CanonicalNameFactory.stringName
            )
            "java.lang.Byte" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "java.lang.Short" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "java.lang.Integer" -> DataType(
                    if (isSingular) DataTypeDescriptor.INTEGER else DataTypeDescriptor.INTEGERS,
                    CanonicalNameFactory.integerName
            )
            "java.lang.Long" -> DataType(
                    if (isSingular) DataTypeDescriptor.LONG else DataTypeDescriptor.LONGS,
                    CanonicalNameFactory.longName
            )
            "java.lang.Float" -> DataType(
                    if (isSingular) DataTypeDescriptor.FLOAT else DataTypeDescriptor.FLOATS,
                    CanonicalNameFactory.floatName
            )
            "java.lang.Double" -> DataType(
                    if (isSingular) DataTypeDescriptor.DOUBLE else DataTypeDescriptor.DOUBLES,
                    CanonicalNameFactory.doubleName
            )
            "java.lang.String" -> DataType(
                    if (isSingular) DataTypeDescriptor.STRING else DataTypeDescriptor.STRINGS,
                    CanonicalNameFactory.stringName
            )
            else -> DataType(
                    if (isSingular) DataTypeDescriptor.REFERENCE else DataTypeDescriptor.REFERENCES,
                    CanonicalNameFactory.newTypeName(e.baseClassName)
            )
        }
    }
}