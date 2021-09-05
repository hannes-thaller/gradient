package org.sourceflow.gradient.sensor

import io.kotest.core.spec.style.StringSpec
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import mu.KotlinLogging
import org.objectweb.asm.ClassReader
import org.objectweb.asm.util.TraceClassVisitor
import org.sourceflow.gradient.code.CodeEntity
import org.sourceflow.gradient.common.CommonEntity
import org.sourceflow.gradient.sensor.code.CodeAnalysis
import org.sourceflow.gradient.sensor.entity.CanonicalName
import org.sourceflow.gradient.sensor.entity.CanonicalNameFactory
import org.sourceflow.gradient.sensor.entity.CodeElementGraph
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus
import org.sourceflow.gradient.sensor.monitoring.TypeTransformer
import org.sourceflow.gradient.sensor.persistence.MonitoringDao
import java.io.IOException
import java.io.PrintWriter

private val logger = KotlinLogging.logger { }

class SensorIntegrationTest : StringSpec() {
    init {
        "should generate events in database"{
            val projectContext = projectRegistrationStep()
            assertProjectRegistrationStep(projectContext)

            val (elementGraph, elementUpdates) = codeAnalysisStep(projectContext)
            assertCodeAnalysisStep(elementGraph, elementUpdates)

            val (classes, cls) = monitorWeavingStep(elementGraph)
            assertMonitoringWeavingStep(cls)
            ClassReader(classes.getValue("org.sourceflow.gradient.sensor.test.MonitoringSut"))
                    .accept(TraceClassVisitor(PrintWriter("report/SensorIntegrationTestByteCode.txt")), ClassReader.EXPAND_FRAMES)


            val dao = invokeTransformedClassStep(projectContext, cls)
            assertTransformedClassStep(dao)
        }
    }

    private suspend fun projectRegistrationStep(): CommonEntity.ProjectContext {
        logger.debug { "PROJECT REGISTRATION STEP" }

        val projectName = CanonicalNameFactory.newProjectName("org.sourceflow.gradient.sensor.test", "MonitoringSut", "0.1.0")
        return DIContainer.projectDao.registerProject(projectName)
    }

    private fun assertProjectRegistrationStep(projectContext: CommonEntity.ProjectContext) {
        projectContext.projectId.shouldNotBeNull()
        projectContext.sessionId.shouldNotBeNull()
    }

    private suspend fun codeAnalysisStep(projectContext: CommonEntity.ProjectContext): Pair<CodeElementGraph, List<CodeEntity.CodeElementModelUpdate>> {
        logger.debug { "CODE ANALYSIS STEP" }

        val packageRegex = listOf("org.sourceflow.gradient.sensor.test", "java.lang", "java.io", "java.util")
        val classRegex = listOf("org.sourceflow.gradient.sensor.test.*", "java.lang.System", "java.io.PrintStream")
        val universeRegex = listOf("org.sourceflow.gradient.sensor.test.*")
        val boundaryRegex = listOf("org.sourceflow.gradient.sensor.test.*", "java.io.PrintStream.print.*Long.*")

        val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex, boundaryRegex)
        val modelElements = DIContainer.codeDao.reportElementGraph(projectContext, elementGraph)
        elementGraph.addCodeElementsToModelUniverse(modelElements.map { it.elementId })

        return Pair(elementGraph, modelElements)
    }

    private fun assertCodeAnalysisStep(elementGraph: CodeElementGraph, elementUpdates: List<CodeEntity.CodeElementModelUpdate>) {
        elementGraph.types.forAny { type ->
            canonicalName(type.name) shouldBe "org.sourceflow.gradient.sensor.test.CallerClass"
            type.status shouldBe ModelingUniverseStatus.INTERNAL_MODEL
        }
        elementGraph.types.forAny { type ->
            canonicalName(type.name) shouldBe "org.sourceflow.gradient.sensor.test.CalleeClass"
            type.status shouldBe ModelingUniverseStatus.INTERNAL_MODEL
        }
        // TODO add more assertions
    }

    private fun monitorWeavingStep(elementGraph: CodeElementGraph): Pair<Map<String, ByteArray>, Class<*>> {
        val codeElements = elementGraph.nameMap()
        val classes = elementGraph.typesAndInfo()
                .filter { it.second.status == ModelingUniverseStatus.INTERNAL_MODEL }
                .map { (info, _) ->
                    info.name to TypeTransformer.transform(info.name, codeElements)
                }
                .toMap()

        val classLoader = TestClassLoader(classes)
        Thread.currentThread().contextClassLoader = classLoader

        val cls = classLoader.loadClass("org.sourceflow.gradient.sensor.test.MonitoringSut")
        return Pair(classes, cls)
    }

    private fun assertMonitoringWeavingStep(cls: Class<*>) {
        cls.canonicalName shouldBe "org.sourceflow.gradient.sensor.test.MonitoringSut"
        cls.declaredMethods shouldHaveSize 13
        cls.declaredFields shouldHaveSize 2
        cls.declaredMethods.forAny {
            it.name shouldBe "getField"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "incField"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "addToField"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "local"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "externalExcluded"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "externalIncluded"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "stringList"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "stringArray"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "primitiveList"
        }
        cls.declaredMethods.forAny {
            it.name shouldBe "inheritanceCall"
        }
    }

    private fun invokeTransformedClassStep(projectContext: CommonEntity.ProjectContext, cls: Class<*>): MonitoringDao {
        val dao = DIContainer.monitoringDao
        dao.reportOn(projectContext)

        val obj = cls.getDeclaredConstructor(Int::class.java).newInstance(14)
        cls.getDeclaredMethod("getField").invoke(obj)
        cls.getDeclaredMethod("incField").invoke(obj)
        cls.getDeclaredMethod("addToField", Int::class.java).invoke(obj, 1)
        cls.getDeclaredMethod("local", Int::class.java).invoke(obj, 1)
        cls.getDeclaredMethod("externalExcluded", Long::class.java).invoke(obj, 1L)
        cls.getDeclaredMethod("externalIncluded", Long::class.java).invoke(obj, 7L)
        cls.getDeclaredMethod("stringList", List::class.java).invoke(obj, listOf("hello", "world"))
        cls.getDeclaredMethod("stringArray", Array<String>::class.java).invoke(obj, arrayOf("hello", "world"))
        cls.getDeclaredMethod("primitiveList", List::class.java).invoke(obj, listOf(0f, 1f))
        cls.getDeclaredMethod("inheritanceCall").invoke(obj)
        try {
            cls.getDeclaredMethod("exception", String::class.java).invoke(obj, "not a real file")
        } catch (ex: IOException) {
        }
        cls.getDeclaredMethod("name").invoke(null)
        try {
            cls.getDeclaredMethod("throwing", String::class.java).invoke(obj, "hell")
        } catch (ex: Exception) {

        }

        dao.reportStop()
        return dao
    }

    private fun assertTransformedClassStep(dao: MonitoringDao) {
        dao.getMessagesSend() shouldBe  27L
        println(dao.getMessagesSend())
    }

    private fun canonicalName(e: CanonicalName): String {
        return e.components.joinToString(".") { it.value }
    }
}