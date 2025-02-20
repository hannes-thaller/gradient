package org.sourceflow.gradient.sensor.monitoring

import mu.KotlinLogging
import org.objectweb.asm.Type
import org.sourceflow.gradient.sensor.entity.CanonicalName
import org.sourceflow.gradient.sensor.entity.CodeElement
import java.lang.instrument.ClassFileTransformer
import java.security.ProtectionDomain

private val logger = KotlinLogging.logger { }


class ClassFileTransformer(
    private val inUniverseTypeNames: List<String>,
    private val codeElements: Map<CanonicalName, CodeElement>
) : ClassFileTransformer {
    override fun transform(
        loader: ClassLoader, className: String, classBeingRedefined: Class<*>,
        protectionDomain: ProtectionDomain, classfileBuffer: ByteArray
    ): ByteArray {
        val name = Type.getObjectType(className).className

        return if (className in inUniverseTypeNames) {
            logger.trace { "Transforming $name" }
            TypeTransformer.transform(classfileBuffer, name, codeElements)
        } else {
            classfileBuffer
        }
    }

    override fun transform(
        module: Module?,
        loader: ClassLoader?,
        className: String,
        classBeingRedefined: Class<*>?,
        protectionDomain: ProtectionDomain?,
        classfileBuffer: ByteArray
    ): ByteArray {
        val name = Type.getObjectType(className).className

        return if (name in inUniverseTypeNames) {
            logger.trace { "Transforming $name" }
            TypeTransformer.transform(classfileBuffer, name, codeElements)
        } else {
            classfileBuffer
        }
    }
}