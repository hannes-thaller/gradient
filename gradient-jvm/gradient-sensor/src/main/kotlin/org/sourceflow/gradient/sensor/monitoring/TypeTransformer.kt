package org.sourceflow.gradient.sensor.monitoring

import mu.KotlinLogging
import org.objectweb.asm.*
import org.sourceflow.gradient.sensor.entity.*

private val logger = KotlinLogging.logger { }


internal class TypeTransformer private constructor(
    next: ClassVisitor,
    private var typeName: CanonicalName,
    private val codeElements: Map<CanonicalName, CodeElement>
) :
    ClassVisitor(Opcodes.ASM7, next) {
    companion object {
        fun transform(
            className: String,
            codeElements: Map<CanonicalName, CodeElement>
        ): ByteArray {
            val typeName = CanonicalNameFactory.newTypeName(className)

            val reader = ClassReader(className)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
            val generator = TypeTransformer(writer, typeName, codeElements)
            reader.accept(generator, ClassReader.EXPAND_FRAMES)

            return writer.toByteArray()
        }

        fun transform(
            bytes: ByteArray,
            className: String,
            codeElements: Map<CanonicalName, CodeElement>
        ): ByteArray {
            val typeName = CanonicalNameFactory.newTypeName(className)

            val reader = ClassReader(bytes)
            val writer = ClassWriter(reader, ClassWriter.COMPUTE_FRAMES)
            val generator = TypeTransformer(writer, typeName, codeElements)
            reader.accept(generator, ClassReader.EXPAND_FRAMES)

            val bytesNew = writer.toByteArray()
            logger.debug { "$className original = ${bytes.size}, transformed = ${bytesNew.size}" }

            return bytesNew
        }
    }

    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val methodName = CanonicalNameFactory.newMethodName(typeName, name, descriptor)
        val executable = codeElements[methodName] as? Executable

        var nextVisitor: MethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (executable != null && executable.status == ModelingUniverseStatus.INTERNAL_MODEL) {
            nextVisitor = if (executable.invokes.any { it.status == ModelingUniverseStatus.BOUNDARY_MODEL }) {
                val boundaryTransformer = BoundaryExecutableTransformer(
                    executable, nextVisitor, access, name, descriptor,
                    signature, exceptions
                )
                ExecutableTransformer(executable, boundaryTransformer, access, name, descriptor)
            } else {
                ExecutableTransformer(executable, nextVisitor, access, name, descriptor)
            }
        }

        return nextVisitor
    }
}