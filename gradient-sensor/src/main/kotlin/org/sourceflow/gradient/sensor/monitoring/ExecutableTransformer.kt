package org.sourceflow.gradient.sensor.monitoring

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.GeneratorAdapter
import org.sourceflow.gradient.sensor.entity.CanonicalNameFactory
import org.sourceflow.gradient.sensor.entity.Executable
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus


class ExecutableTransformer(
        private val executable: Executable,
        mv: MethodVisitor,
        access: Int,
        name: String,
        private val descriptor: String
) : GeneratorAdapter(Opcodes.ASM7, mv, access, name, descriptor) {
    var frameId: Int = -1

    override fun visitCode() {
        super.visitCode()
        frameId = newLocal(Type.LONG_TYPE)
        push(executable.id)
        invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.frameMethod)
        storeLocal(frameId)
        handleParameters()
    }

    private fun handleParameters() {
        for ((type, param) in Type.getArgumentTypes(descriptor) zip executable.parameters) {
            if (param.status == ModelingUniverseStatus.INTERNAL_MODEL) {
                loadArg(param.index)
                push(executable.id)
                push(param.id)
                loadLocal(frameId)
                invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.receiveV(type))
            }
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        val targetName = CanonicalNameFactory.newPropertyName(
            CanonicalNameFactory.newTypeName(Type.getObjectType(owner).className), name
        )
        val type = Type.getType(descriptor)
        var transform = { super.visitFieldInsn(opcode, owner, name, descriptor) }
        when (opcode) {
            Opcodes.GETFIELD, Opcodes.GETSTATIC -> {
                executable.reads
                    .find { it.name == targetName && it.status == ModelingUniverseStatus.INTERNAL_MODEL }
                    ?.let {
                        transform = {
                            super.visitFieldInsn(opcode, owner, name, descriptor)
                            push(executable.id)
                            push(it.id)
                            loadLocal(frameId)
                            invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.read(type))
                            if (type.sort in Type.ARRAY..Type.OBJECT) checkCast(type)
                        }
                    }
            }
            Opcodes.PUTFIELD, Opcodes.PUTSTATIC -> {
                executable.writes
                    .find { it.name == targetName && it.status == ModelingUniverseStatus.INTERNAL_MODEL }
                    ?.let {
                        transform = {
                            push(executable.id)
                            push(it.id)
                            loadLocal(frameId)
                            invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.write(type))
                            if (type.sort in Type.ARRAY..Type.OBJECT) checkCast(type)
                            super.visitFieldInsn(opcode, owner, name, descriptor)
                        }
                    }
            }
        }

        transform()
    }

    override fun visitInsn(opcode: Int) {
        when (opcode) {
            in Opcodes.IRETURN..Opcodes.ARETURN -> {
                push(executable.id)
                loadLocal(frameId)
                invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.returns(returnType))
                if (returnType.sort in Type.ARRAY..Type.OBJECT) checkCast(returnType)
            }
            Opcodes.RETURN -> {
                push(executable.id)
                loadLocal(frameId)
                invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.returns())
            }
            Opcodes.ATHROW -> {
                push(executable.id)
                loadLocal(frameId)
                invokeStatic(MonitoringDaoClassInfo.facade, MonitoringDaoClassInfo.except)
            }
        }

        super.visitInsn(opcode)
    }
}