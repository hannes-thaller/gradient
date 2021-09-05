package org.sourceflow.gradient.sensor.monitoring

import org.objectweb.asm.Type
import org.objectweb.asm.commons.Method

internal object MonitoringDaoClassInfo {
    val facade: Type = Type.getType(ByteCodeFacade::class.java)
    val frameMethod: Method = Method.getMethod(ByteCodeFacade::class.java.getDeclaredMethod("frame", Int::class.java))
    val except: Method = Method.getMethod(ByteCodeFacade::class.java.getDeclaredMethod("except", Int::class.java, Long::class.java))
    private val voidReturns = Method.getMethod(ByteCodeFacade::class.java.getDeclaredMethod("returnsV", Int::class.java, Long::class.java))
    private val voidReturnsTarget = Method.getMethod(ByteCodeFacade::class.java.getDeclaredMethod("returnsV", Int::class.java, Int::class.java, Long::class.java))

    private val objectType = Type.getType(Object::class.java)
    private val receives: Map<Type, Method>
    private val receivesV: Map<Type, Method>
    private val reads: Map<Type, Method>
    private val writes: Map<Type, Method>
    private val returns: Map<Type, Method>
    private val returnsTarget: Map<Type, Method>

    init {
        val types = mapOf(
                Type.BOOLEAN_TYPE to Boolean::class.java,
                Type.CHAR_TYPE to Char::class.java,
                Type.BYTE_TYPE to Byte::class.java,
                Type.SHORT_TYPE to Short::class.java,
                Type.INT_TYPE to Int::class.java,
                Type.FLOAT_TYPE to Float::class.java,
                Type.LONG_TYPE to Long::class.java,
                Type.DOUBLE_TYPE to Double::class.java,
                Type.getType(Object::class.java) to Object::class.java
        )
        val sourceTargetArguments: Array<Class<*>> = arrayOf(Int::class.java, Int::class.java, Long::class.java)
        receives = createDaoMethods("receive", types, sourceTargetArguments)
        receivesV = createDaoMethods("receiveV", types, sourceTargetArguments)
        reads = createDaoMethods("read", types, sourceTargetArguments)
        writes = createDaoMethods("write", types, sourceTargetArguments)

        returns = createDaoMethods("returns", types, arrayOf(Int::class.java, Long::class.java))
        returnsTarget = createDaoMethods("returns", types, sourceTargetArguments)
    }

    private fun createDaoMethods(
            methodName: String,
            types: Map<Type, Class<*>>,
            argumentTypes: Array<Class<*>>
    ): Map<Type, Method> {
        return types.mapValues { (_, cls) ->
            Method.getMethod(
                    ByteCodeFacade::class.java.getDeclaredMethod(methodName, cls, *argumentTypes)
            )
        }
    }

    fun receive(type: Type): Method {
        return if (type in receives) {
            receives.getValue(type)
        } else {
            receives.getValue(objectType)
        }
    }

    fun receiveV(type: Type): Method {
        return if (type in receives) {
            receivesV.getValue(type)
        } else {
            receivesV.getValue(objectType)
        }
    }

    fun read(type: Type): Method {
        return if (type in reads) {
            reads.getValue(type)
        } else {
            reads.getValue(objectType)
        }
    }

    fun write(type: Type): Method {
        return if (type in writes) {
            writes.getValue(type)
        } else {
            writes.getValue(objectType)
        }
    }

    fun returns(type: Type): Method {
        return when (type) {
            in returns -> returns.getValue(type)
            Type.VOID_TYPE -> voidReturns
            else -> returns.getValue(objectType)
        }
    }

    fun returns(): Method {
        return voidReturns
    }

    fun returnsTarget(type: Type): Method {
        return when (type) {
            in returnsTarget -> returnsTarget.getValue(type)
            Type.VOID_TYPE -> voidReturnsTarget
            else -> returnsTarget.getValue(objectType)
        }
    }
}
