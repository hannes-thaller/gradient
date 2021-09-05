package org.sourceflow.gradient.sensor.monitoring

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import org.sourceflow.gradient.sensor.DIContainer
import org.sourceflow.gradient.sensor.TestClassLoader
import org.sourceflow.gradient.sensor.code.CodeAnalysis
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus
import org.sourceflow.gradient.sensor.persistence.MonitoringDao
import java.io.PrintWriter

class EventAnalysisTest : StringSpec({
    val packageRegex = listOf("org.sourceflow.gradient.sensor.test", "java.lang", "java.io", "java.util")
    val classRegex = listOf("org.sourceflow.gradient.sensor.test.*", "java.lang.System", "java.io.PrintStream")
    val universeRegex = listOf("org.sourceflow.gradient.sensor.test.*")
    val modelElements = setOf(
            "org.sourceflow.gradient.sensor.test.MonitoringSut",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.field",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.<init>(java.lang.Integer): java.lang.Void",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.<init>(java.lang.Integer): java.lang.Void | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.getField(): java.lang.Integer",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.incField(): java.lang.Integer",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.addToField(java.lang.Integer): java.lang.Integer",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.addToField(java.lang.Integer): java.lang.Integer | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.local(java.lang.Integer): java.lang.Integer",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.local(java.lang.Integer): java.lang.Integer | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.externalExcluded(java.lang.Long): java.lang.Void",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.externalExcluded(java.lang.Long): java.lang.Void | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.externalIncluded(java.lang.Long): java.lang.Void",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.externalIncluded(java.lang.Long): java.lang.Void | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.exception(java.lang.String): java.lang.Void",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.exception(java.lang.String): java.lang.Void | param0",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.throwing(java.lang.String): java.lang.Void",
            "org.sourceflow.gradient.sensor.test.MonitoringSut.throwing(java.lang.String): java.lang.Void | param0"

    )
    val externalModelElements = mutableListOf(
            "java.io.PrintStream.print(java.lang.Long): java.lang.Void",
            "java.io.PrintStream.print(java.lang.Long): java.lang.Void | param0"
    )

    val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex).also { graph ->
        graph.codeElements()
                .forEach {
                    when (it.name.toString()) {
                        in modelElements -> it.status = ModelingUniverseStatus.INTERNAL_MODEL
                        in externalModelElements -> it.status = ModelingUniverseStatus.BOUNDARY_MODEL
                        else -> {
                        }
                    }
                }
    }

    val codeElements = elementGraph.nameMap()
    val classes = elementGraph.typesAndInfo()
            .filter { it.second.status == ModelingUniverseStatus.INTERNAL_MODEL }
            .map { (info, _) ->
                info.name to TypeTransformer.transform(info.name, codeElements)
            }
            .toMap()

    ClassReader(classes.getValue("org.sourceflow.gradient.sensor.test.MonitoringSut"))
            .accept(TraceClassVisitor(PrintWriter("EventAnalysisTestByteCode.txt")), ClassReader.EXPAND_FRAMES)

    val classLoader = TestClassLoader(classes)
    Thread.currentThread().contextClassLoader = classLoader

    val ids = elementGraph.nameMap().map { (name, element) -> name.toString() to element }

    unmockkAll()
    mockkStatic(ByteCodeFacade::class)
    mockkObject(DIContainer)
    every { DIContainer.monitoringDao } returns mockk(relaxed = true)
    val clazz: Class<*> = classLoader.loadClass("org.sourceflow.gradient.sensor.test.MonitoringSut")

    afterSpec {
        unmockkAll()
    }

    "should monitor <init>"{
        clearStaticMockk(ByteCodeFacade::class)
        clazz.getDeclaredConstructor(Int::class.java).newInstance(14)

        val source = ids.find { "MonitoringSut.<init>" in it.first }!!.second.id
        val paramTarget = ids.find { "MonitoringSut.<init>" in it.first && "| param0" in it.first }!!.second.id
        val fieldTarget = ids.find { "MonitoringSut.field" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV(14, source, paramTarget, any())
            ByteCodeFacade.write(14, source, fieldTarget, any())
            ByteCodeFacade.returnsV(source, any())
            ByteCodeFacade.returnsV(source, 0, any())
        }
    }

    "should monitor getField"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("getField")

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut) shouldBe 14

        val source = ids.find { "MonitoringSut.getField" in it.first }!!.second.id
        val target = ids.find { "MonitoringSut.field" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.read(14, source, target, any())
            ByteCodeFacade.returns(14, source, any())
            ByteCodeFacade.returns(14, source, 0, any())
        }
    }

    "should monitor incField"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("incField")

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut) shouldBe 15

        val source = ids.find { "incField" in it.first }!!.second.id
        val target = ids.find { "MonitoringSut.field" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.read(14, source, target, any())
            ByteCodeFacade.write(15, source, target, any())
            ByteCodeFacade.returns(15, source, any())
            ByteCodeFacade.returns(15, source, 0, any())
        }
    }

    "should monitor addToField"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("addToField", Int::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut, 2) shouldBe 16

        val source = ids.find { "MonitoringSut.addToField" in it.first }!!.second.id
        val getSource = ids.find { "MonitoringSut.getField" in it.first }!!.second.id
        val paramTarget = ids.find { "MonitoringSut.addToField" in it.first && "| param0" in it.first }!!.second.id
        val fieldTarget = ids.find { "MonitoringSut.field" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV(2, source, paramTarget, any())
            ByteCodeFacade.read(14, source, fieldTarget, any())
            ByteCodeFacade.write(16, source, fieldTarget, any())
            ByteCodeFacade.frame(getSource)
            ByteCodeFacade.read(16, getSource, fieldTarget, any())
            ByteCodeFacade.returns(16, getSource, any())
            ByteCodeFacade.returns(16, getSource, 0, any())
            ByteCodeFacade.returns(16, source, any())
            ByteCodeFacade.returns(16, source, 0, any())
        }
    }

    "should monitor local"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("local", Int::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut, 2) shouldBe 3

        val source = ids.find { "local" in it.first }!!.second.id
        val paramTarget = ids.find { "local" in it.first && "| param0" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV(2, source, paramTarget, any())
            ByteCodeFacade.returns(3, source, any())
            ByteCodeFacade.returns(3, source, 0, any())
        }
    }

    "should monitor externalExcluded"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("externalExcluded", Long::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut, 2L)

        val source = ids.find { "externalExcluded" in it.first }!!.second.id
        val paramTarget = ids.find { "externalExcluded" in it.first && "| param0" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV(2L, source, paramTarget, any())
            ByteCodeFacade.returnsV(source, any())
            ByteCodeFacade.returnsV(source, 0, any())
        }
    }

    "should monitor externalIncluded".config(enabled=false){
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("externalIncluded", Long::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut, 2L)

        val source = ids.find { "MonitoringSut.externalIncluded" in it.first }!!.second.id
        val paramTarget =
                ids.find { "MonitoringSut.externalIncluded" in it.first && "| param0" in it.first }!!.second.id
        val externalSource = ids.find { "PrintStream.print(java.lang.Long" in it.first }!!.second.id
        val externalParamTarget =
                ids.find { "PrintStream.print(java.lang.Long" in it.first && "| param0" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV(2L, source, paramTarget, any())
            ByteCodeFacade.receive(2L, externalSource, externalParamTarget, any())
            ByteCodeFacade.returnsV(externalSource, source, any())
            ByteCodeFacade.returnsV(source, any())
            ByteCodeFacade.returnsV(source, 0, any())
        }
    }

    "should monitor exceptions"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("exception", String::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        method.invoke(sut, "invalidFile")

        val source = ids.find { "MonitoringSut.exception" in it.first }!!.second.id
        val paramTarget =
                ids.find { "MonitoringSut.exception" in it.first && "| param0" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV("invalidFile", source, paramTarget, any())
            ByteCodeFacade.returnsV(source, any())
            ByteCodeFacade.returnsV(source, 0, any())
        }
    }

    "should monitor throwing"{
        val sut = clazz.getDeclaredConstructor(Int::class.java).newInstance(14)
        val method = clazz.getDeclaredMethod("throwing", String::class.java)

        clearStaticMockk(ByteCodeFacade::class)
        try {
            method.invoke(sut, "invalidFile")
        } catch (ex: Exception) {
        }

        val source = ids.find { "MonitoringSut.throwing" in it.first }!!.second.id
        val paramTarget =
                ids.find { "MonitoringSut.throwing" in it.first && "| param0" in it.first }!!.second.id

        verify(Ordering.ORDERED) {
            ByteCodeFacade.frame(source)
            ByteCodeFacade.receiveV("invalidFile", source, paramTarget, any())
            ByteCodeFacade.except(source, any())
        }
    }
})