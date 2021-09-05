package org.sourceflow.asm

import io.kotest.core.spec.style.StringSpec
import org.objectweb.asm.*
import org.objectweb.asm.commons.GeneratorAdapter
import org.objectweb.asm.commons.Method
import org.objectweb.asm.util.TraceClassVisitor
import org.sourceflow.gradient.sensor.TestClassLoader
import java.io.PrintStream
import java.io.PrintWriter

class TestClass {
    fun method(a: Long): Long {
        var sum = 0L
        for (it in 0 until a) {
            sum += it
        }

        return sum
    }
}


class LocalVariableTest : StringSpec({

    "local variable sorter should add new local variable at start"{

        val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
        val tracer = TraceClassVisitor(cw, PrintWriter(System.out))

        val transformer = object : ClassVisitor(Opcodes.ASM7, tracer) {
            override fun visitMethod(
                    access: Int,
                    name: String?,
                    descriptor: String?,
                    signature: String?,
                    exceptions: Array<out String>?
            ): MethodVisitor {
                val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)
                return object : GeneratorAdapter(Opcodes.ASM7, mv, access, name, descriptor) {
                    var variable: Int = -1
                    override fun visitCode() {
                        super.visitCode()
                        variable = newLocal(Type.LONG_TYPE)
                        invokeStatic(Type.getType(System::class.java), Method.getMethod("long currentTimeMillis()"))
                        storeLocal(variable)
                    }

                    override fun visitInsn(opcode: Int) {
                        if (opcode == Opcodes.LRETURN) {
                            getStatic(Type.getType(System::class.java), "out", Type.getType(PrintStream::class.java))
                            loadLocal(variable)
                            invokeVirtual(Type.getType(PrintStream::class.java), Method.getMethod("void println(long)"))
                        }
                        mv.visitInsn(opcode)
                    }

                    override fun visitEnd() {
                        super.visitEnd()
                    }
                }
            }
        }

        val testClassName = TestClass::class.java.canonicalName
        val reader = ClassReader(testClassName)
        reader.accept(transformer, ClassReader.EXPAND_FRAMES)
        val bytes = cw.toByteArray()
        val loader = TestClassLoader(mutableMapOf(testClassName to bytes))
        val cls = loader.loadClass(testClassName)
        val obj = cls.getDeclaredConstructor().newInstance()
        cls.getDeclaredMethod("method", Long::class.java).invoke(obj, 14)
    }
})