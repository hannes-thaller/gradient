package org.sourceflow.gradient.sensor.monitoring

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import org.sourceflow.gradient.sensor.entity.*
import org.sourceflow.gradient.sensor.entity.CanonicalNameFactory

class BoundaryExecutableTransformer(
        private val executable: Executable,
        mv: MethodVisitor,
        access: Int,
        name: String,
        private val descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
) : MethodNode(Opcodes.ASM7, access, name, descriptor, signature, exceptions) {
    init {
        super.mv = mv
    }

    override fun visitEnd() {
        val frameId = LocalVariableNode("__frameId__", "J", null, LabelNode(), LabelNode(), frameIdIndex())
        transformBoundary(frameId)
        accept(mv)
    }

    private fun frameIdIndex(): Int {
        var nextIndex = if (executable.isClassMember) 0 else 1
        for (argumentType in Type.getArgumentTypes(descriptor)) {
            nextIndex += argumentType.size
        }

        return nextIndex
    }

    private fun transformBoundary(frameId: LocalVariableNode) {
        var it: AbstractInsnNode? = instructions.first
        while (it != null) {
            if (it is MethodInsnNode) {
                it = handleMethodNode(it, frameId)
            }
            it = it.next
        }
    }

    private fun handleMethodNode(currentNode: MethodInsnNode, frameId: LocalVariableNode): AbstractInsnNode {
        if (currentNode.owner == "java/lang/Object" && currentNode.name == "<init>" ||
            currentNode.owner == "java/lang/Object" && currentNode.name == "<clinit>"
        ) return currentNode

        val targetName = CanonicalNameFactory.newMethodName(
            CanonicalNameFactory.newTypeName(Type.getObjectType(currentNode.owner).className),
            currentNode.name, currentNode.desc
        )
        val targetElement = executable.invokes.find { it.name == targetName }

        var lastNode: AbstractInsnNode = currentNode
        if (targetElement != null && targetElement.status == ModelingUniverseStatus.BOUNDARY_MODEL) {
            lastNode = handleBoundaryMethodNode(targetElement, currentNode, frameId)
        }

        return lastNode
    }

    private fun handleBoundaryMethodNode(
            target: Executable,
            currentNode: MethodInsnNode,
            frameIdVar: LocalVariableNode
    ): AbstractInsnNode {
        assert(target.parameters.size == Type.getArgumentTypes(currentNode.desc).size) {
            "Number of analyzed parameters is different"
        }
        val parameters = (target.parameters zip Type.getArgumentTypes(currentNode.desc))
            .reversed()
            .iterator()

        val lastNode = currentNode.next
        val il = mutableListOf<AbstractInsnNode>()
        il.addAll(returnInstructions(target, currentNode.desc, frameIdVar).reversed())
        il.add(currentNode)

        var it: AbstractInsnNode? = currentNode.previous
        instructions.remove(currentNode)
        while (it != null && parameters.hasNext()) {
            val (parameter, type) = parameters.next()
            if (parameter.status == ModelingUniverseStatus.BOUNDARY_MODEL) {
                il.addAll(parameterInstructions(target, parameter, type.descriptor, frameIdVar).reversed())
            }
            il.add(it)
            it = it.previous
            instructions.remove(il.last())
        }

        val instructions = InsnList()
        il.forEach { instructions.insert(it) }
        this.instructions.insertBefore(lastNode, instructions)

        return lastNode.previous
    }

    private fun parameterInstructions(
            source: Executable,
            target: Parameter,
            descriptor: String,
            frameIdVar: LocalVariableNode
    ): List<AbstractInsnNode> {
        val type = Type.getType(descriptor)
        val li = mutableListOf(
            pushElementId(source),
            pushElementId(target),
            pushFrameId(frameIdVar),
            MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "org/sourceflow/gradient/sensor/persistence/EventDao",
                "receive",
                MonitoringDaoClassInfo.receive(type).descriptor,
                false
            )
        )

        if (type.sort in Type.ARRAY..Type.OBJECT) {
            li.add(TypeInsnNode(Opcodes.CHECKCAST, type.internalName))
        }

        return li
    }

    private fun returnInstructions(
            executable: Executable,
            descriptor: String,
            frameIdVar: LocalVariableNode
    ): List<AbstractInsnNode> {
        val type = Type.getReturnType(descriptor)
        val li = mutableListOf(
            pushElementId(executable),
            pushElementId(this.executable),
            pushFrameId(frameIdVar),
            MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "org/sourceflow/gradient/sensor/persistence/EventDao",
                if (type == Type.VOID_TYPE) "returnsV" else "returns",
                MonitoringDaoClassInfo.returnsTarget(type).descriptor,
                false
            )
        )

        if (type.sort in Type.ARRAY..Type.OBJECT) {
            li.add(TypeInsnNode(Opcodes.CHECKCAST, type.internalName))
        }

        return li
    }

    private fun pushElementId(elementId: CodeElement): IntInsnNode {
        return IntInsnNode(if (elementId.id < 128) Opcodes.BIPUSH else Opcodes.SIPUSH, elementId.id)
    }

    private fun pushFrameId(frameIdVar: LocalVariableNode): VarInsnNode {
        return VarInsnNode(Opcodes.LLOAD, frameIdVar.index)
    }
}