//package org.sourceflow.gradient.sensor
//
//import io.kotlintest.shouldBe
//import io.kotlintest.specs.StringSpec
//import org.sourceflow.gradient.sensor.code.CodeAnalysis
//import org.sourceflow.gradient.sensor.analysis.TypeTransformer
//import org.sourceflow.gradient.sensor.persistence.CodeDao
//import org.sourceflow.gradient.sensor.persistence.EventDao
//import org.sourceflow.gradient.sensor.entity.CanonicalNameFactory
//import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus
//import org.sourceflow.gradient.sensor.entity.Project
//
//class NutritionAdvisorTest : StringSpec() {
//    init {
//        val packageRegex = listOf("org.sourceflow.test", "java.lang", "java.io", "java.util")
//        val classRegex = listOf("org.sourceflow.test.*", "java.lang.System", "java.io.PrintStream")
//        val universeRegex = listOf(
//            "org.sourceflow.test.*"
//        )
//
//        val projectName = CanonicalNameFactory.newProjectName("org.sourceflow.test", "NutritionAdvisor", "0.1.0")
//        val project = Project(projectName)
//
//        "should generate code analysis".config(enabled = false) {
//            val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex)
//            CodeDao.registerProjectVersion(project, elementGraph).let {
//                elementGraph.addInModelElementsForIds(it)
//            }
//        }
//
//        "should generate all events in database".config(enabled = false) {
//
//            val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex)
//            CodeDao.registerProjectVersion(project, elementGraph).let {
//                elementGraph.addInModelElementsForIds(it)
//            }
//            elementGraph.types
//                .filter { "org.sourceflow.test.Main" in it.name.toString() }
//                .forEach { it.status = ModelingUniverseStatus.INTERNAL_MODEL }
//
//            val codeElementsMap = elementGraph.nameMap()
//
//            val classes = elementGraph.typesAndInfo()
//                .filter { it.second.status == ModelingUniverseStatus.INTERNAL_MODEL }
//                .map { (info, _) ->
//                    info.name to TypeTransformer.transform(info.resource.load(), info.name, codeElementsMap)
//                }
//                .toMap()
//
//            val classLoader = TestClassLoader(classes)
//            Thread.currentThread().contextClassLoader = classLoader
//
//            val clazz = classLoader.loadClass("org.sourceflow.test.Main")
//            val obj = clazz.newInstance()
//            val method = clazz.getDeclaredMethod("run")
//
//            EventDao.open(project)
//            method.invoke(obj)
//            EventDao.close()
//
//            EventDao.totalMessagesSend() shouldBe 27002
//        }
//    }
//}