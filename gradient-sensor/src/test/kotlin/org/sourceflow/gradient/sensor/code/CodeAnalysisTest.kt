@file:Suppress("unused")

package org.sourceflow.gradient.sensor.code

import io.kotest.assertions.inspecting
import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.*
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.sourceflow.gradient.sensor.entity.DataTypeDescriptor
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus


class CodeAnalysisTest : StringSpec({
    val packageWhitelist = listOf("org.sourceflow.gradient.sensor.test", "java.lang", "java.io", "java.util")
    val classRegex = listOf("org.sourceflow.gradient.sensor.test.*", "java.lang.System", "java.io.PrintStream")
    val universeRegex = listOf("org.sourceflow.gradient.sensor.test.*")

    val elementGraph = CodeAnalysis.analyzeClasspath(packageWhitelist, classRegex, universeRegex)

    val idMap = elementGraph.codeElements().map { it.name.toString() to it }.toList()
    val types = elementGraph.types
    val properties = elementGraph.properties
    val executables = elementGraph.executables
    val parameters = elementGraph.parameters

    "should find all types"{
        types.forAny {
            it.name.toString() shouldBe "org.sourceflow.gradient.sensor.test.CallerClass"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
            it.properties shouldHaveSize 1
            it.executables shouldHaveSize 7
        }
        types.forAny {
            it.name.toString() shouldBe "org.sourceflow.gradient.sensor.test.CalleeClass"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
            it.properties shouldHaveSize 2
            it.executables shouldHaveAtLeastSize 5
        }
        types.forAny {
            it.name.toString() shouldBe "java.lang.System"
            it.status shouldBe ModelingUniverseStatus.EXTERNAL
            it.properties shouldHaveAtLeastSize 2
        }
    }

    "should find all properties"{
        properties.forAny {
            it.name.toString() shouldBe "org.sourceflow.gradient.sensor.test.CallerClass.callee"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        properties.forAny {
            it.name.toString() shouldBe "org.sourceflow.gradient.sensor.test.CalleeClass.fieldA"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        properties.forAny {
            it.name.toString() shouldBe "org.sourceflow.gradient.sensor.test.CalleeClass.fieldB"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        properties.forAny {
            it.name.toString() shouldBe "java.lang.System.err"
            it.status shouldBe ModelingUniverseStatus.EXTERNAL
        }
    }

    "should find all executables"{
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodA"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodB"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodC"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodD"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodE"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.asm"
            it.status shouldBe ModelingUniverseStatus.EXTERNAL
        }
        executables.forAny {
            it.name.toString() shouldContain "CallerClass.methodA"
            it.status shouldBe ModelingUniverseStatus.INTERNAL
        }
    }

    "should find reads"{
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodA"
            ex.reads shouldContain idMap.find { "CallerClass.callee" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodB"
            ex.reads.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodC"
            ex.reads shouldContain idMap.find { "CallerClass.callee" in it.first }!!.second
            ex.reads shouldNotContain idMap.find { "CalleeClass.fieldA" in it.first }!!.second // property uses getter
            ex.reads shouldNotContain idMap.find { "CalleeClass.fieldB" in it.first }!!.second // property uses getter
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodD"
            ex.reads shouldContain idMap.find { "CallerClass.callee" in it.first }!!.second
            ex.reads shouldNotContain idMap.find { "CalleeClass.fieldA" in it.first }!!.second // property uses getter
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodE"
            ex.reads shouldContain idMap.find { "java.lang.System.err" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodE"
            ex.reads shouldContain idMap.find { "java.lang.System.err" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.asm"
            ex.reads shouldContain idMap.find { "CallerClass.callee" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CalleeClass.methodA"
            ex.reads.shouldBeEmpty()
        }
    }

    "should find writes"{
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodA"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodB"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodC"
            ex.writes.shouldBeEmpty() // property uses setter
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodD"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodE"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodE"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.asm"
            ex.writes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CalleeClass.methodA"
            ex.writes.shouldBeEmpty()
        }
    }

    "should find invokes"{
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodA"
            ex.invokes shouldContain idMap.find { "CalleeClass.methodA" in it.first }!!.second
            ex.invokes shouldContain idMap.find { "CallerClass.methodB" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodB"
            ex.invokes.shouldBeEmpty()
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodC"
            ex.invokes shouldContain idMap.find { "CalleeClass.getFieldA" in it.first }!!.second
            ex.invokes shouldContain idMap.find { "CalleeClass.getFieldB" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodD"
            ex.invokes shouldContain idMap.find { "CalleeClass.setFieldA" in it.first }!!.second // property uses setter
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.methodE"
            ex.invokes shouldHaveSize 1
            ex.invokes shouldContain idMap.find { "java.io.PrintStream.println(java.lang.String)" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CallerClass.asm"
            ex.invokes shouldContain idMap.find { "CalleeClass.getFieldA" in it.first }!!.second
        }
        executables.forAny { ex ->
            ex.name.toString() shouldContain "CalleeClass.methodA"
            ex.invokes.shouldBeEmpty()
            ex.invokes.shouldNotBeNull()
        }
    }

    "should analyze generic parameters"{
        inspecting(
                parameters.find {
                    "stringList" in it.name.toString() && "| param0" in it.name.toString()
                }!!.dataType
        ) {
            dataTypeDescriptor shouldBe DataTypeDescriptor.STRINGS
        }

        inspecting(
                parameters.find {
                    "primitiveList" in it.name.toString() && "| param0" in it.name.toString()
                }!!.dataType
        ) {
            dataTypeDescriptor shouldBe DataTypeDescriptor.FLOATS
        }
    }

    "should analyze generic returns"{
        inspecting(
                executables.find { "stringList" in it.name.toString() }!!.dataType
        ) {
            dataTypeDescriptor shouldBe DataTypeDescriptor.STRINGS
        }

        inspecting(
                parameters.find { "primitiveList" in it.name.toString() }!!.dataType
        ) {
            dataTypeDescriptor shouldBe DataTypeDescriptor.FLOATS
        }
    }
})


