package org.sourceflow.gradient.sensor.code

import io.github.classgraph.ClassInfo
import io.github.classgraph.FieldInfo
import io.github.classgraph.MethodInfo
import org.objectweb.asm.*

internal class DependencyAnalyzer private constructor(
    private val sourceClass: ClassInfo
) : ClassVisitor(Opcodes.ASM7) {
    companion object {
        fun analyze(typeInfo: ClassInfo): List<Dependency> {
            val analyzer = DependencyAnalyzer(typeInfo).also {
                val reader = ClassReader(typeInfo.name)
                reader.accept(it, ClassReader.SKIP_DEBUG)
            }

            return analyzer.dependencies.toList()
        }
    }

    data class Dependency(
            val type: DependencyType,
            val sourceClass: ClassInfo,
            val sourceMethod: MethodInfo,
            val targetClass: ClassInfo,
            val targetMethod: MethodInfo? = null,
            val targetField: FieldInfo? = null
    )

    enum class DependencyType {
        INVOKE, READ, WRITE
    }

    private val targetClassInfos: Map<String, ClassInfo>
    val dependencies = mutableSetOf<Dependency>()

    init {
        targetClassInfos = mutableMapOf<String, ClassInfo>().also { map ->
            map[sourceClass.name] = sourceClass
            sourceClass.classDependencies.associateByTo(map) { it.name }
            sourceClass.superclasses.associateByTo(map) { it.name }
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val sourceMethod = sourceClass.getMethodInfo(name)
            .find { it.typeDescriptorStr == descriptor }
        requireNotNull(sourceMethod)

        return object : MethodVisitor(Opcodes.ASM7) {
            override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                val type = when (opcode) {
                    Opcodes.GETFIELD, Opcodes.GETSTATIC -> DependencyType.READ
                    Opcodes.PUTFIELD, Opcodes.PUTSTATIC -> DependencyType.WRITE
                    else -> error("Unknown field instruction opcode.")
                }

                targetClassInfos[Type.getObjectType(owner).className]?.let { targetClass ->
                    val fieldInfos = targetClass.fieldInfo.toMutableSet()
                    targetClass.subclasses
                        .filterNot { it.isExternalClass }
                        .flatMapTo(fieldInfos) { it.fieldInfo }

                    fieldInfos
                        .filter { it.name == name && it.typeDescriptorStr == descriptor }
                        .mapTo(dependencies) { targetField ->
                            Dependency(type, sourceClass, sourceMethod, targetClass, targetField = targetField)
                        }
                }
            }

            override fun visitMethodInsn(
                opcode: Int,
                owner: String,
                name: String,
                descriptor: String,
                isInterface: Boolean
            ) {
                targetClassInfos[Type.getObjectType(owner).className]?.let { targetClass ->
                    val methodInfos = mutableSetOf<MethodInfo>()
                    if (name == "<init>") {
                        methodInfos.addAll(targetClass.constructorInfo)
                        targetClass.subclasses
                            .filterNot { it.isExternalClass }
                            .flatMapTo(methodInfos) { it.constructorInfo }
                    } else {
                        methodInfos.addAll(targetClass.getMethodInfo(name))
                        targetClass.subclasses
                            .filterNot { it.isExternalClass }
                            .flatMapTo(methodInfos) {
                                it.methodInfo
                            }
                    }

                    methodInfos
                        .filter {
                            it.name == name &&
                                    it.typeDescriptorStr == descriptor
                        }
                        .mapTo(dependencies) { targetInfo ->
                            Dependency(
                                    DependencyType.INVOKE,
                                    sourceClass,
                                    sourceMethod,
                                    targetClass,
                                    targetMethod = targetInfo
                            )
                        }
                }
            }
        }
    }
}