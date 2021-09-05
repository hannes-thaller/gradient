package org.sourceflow.gradient.sensor.monitoring

import io.kotest.core.spec.style.StringSpec
import org.sourceflow.gradient.sensor.TestClassLoader
import org.sourceflow.gradient.sensor.code.CodeAnalysis
import org.sourceflow.gradient.sensor.entity.Executable
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus
import org.sourceflow.gradient.sensor.entity.Parameter
import org.sourceflow.gradient.sensor.entity.Property

class EventAnalysisTestComplex : StringSpec() {
    init {
        val packageRegex = listOf("org.sourceflow.gradient.sensor.test", "java.lang", "java.io", "java.util")
        val classRegex =
                listOf("org.sourceflow.gradient.sensor.test.*", "java.lang.System", "java.io.PrintStream", "java.lang.AssertionError")
        val universeRegex = listOf("org.sourceflow.gradient.sensor.test.*")
        val modelElements = setOf(
                "org.sourceflow.gradient.sensor.test.BmiService.bmi(java.lang.Float, java.lang.Float): java.lang.Float",
                "org.sourceflow.gradient.sensor.test.BmiService.bmi(java.lang.Float, java.lang.Float): java.lang.Float | param0",
                "org.sourceflow.gradient.sensor.test.BmiService.bmi(java.lang.Float, java.lang.Float): java.lang.Float | param1",
                "org.sourceflow.gradient.sensor.test.NutritionAdvisor.advice(org.sourceflow.gradient.sensor.test.Person): java.lang.String",
                "org.sourceflow.gradient.sensor.test.Person.name",
                "org.sourceflow.gradient.sensor.test.Person.age",
                "org.sourceflow.gradient.sensor.test.Person.weight",
                "org.sourceflow.gradient.sensor.test.Person.height",
                "org.sourceflow.gradient.sensor.test.Person.gender",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param0",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param1",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param2",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param3",
                "org.sourceflow.gradient.sensor.test.Person.<init>(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param4",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param0",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param1",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param2",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param3",
                "org.sourceflow.gradient.sensor.test.Servlet.handleRequest(java.lang.String, java.lang.String, java.lang.Integer, java.lang.Float, java.lang.Float): java.lang.Void | param4"
        )

        val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex)
        elementGraph.codeElements().forEach {
            when {
                it is Property && it.name.toString() in modelElements -> it.status =
                        ModelingUniverseStatus.INTERNAL_MODEL
                it is Executable && it.name.toString() in modelElements -> it.status =
                        ModelingUniverseStatus.INTERNAL_MODEL
                it is Parameter && it.name.toString() in modelElements -> it.status =
                        ModelingUniverseStatus.INTERNAL_MODEL
            }
        }
        val codeElements = elementGraph.nameMap()
        val classes = elementGraph.typesAndInfo()
                .filter { it.second.status == ModelingUniverseStatus.INTERNAL_MODEL }
                .map { (info, _) ->
                    info.name to TypeTransformer.transform(info.resource.load(), info.name, codeElements)
                }
                .toMap()

        val classLoader = TestClassLoader(classes)
        Thread.currentThread().contextClassLoader = classLoader
        val clazz: Class<*> = classLoader.loadClass("org.sourceflow.gradient.sensor.test.Servlet")

        "should generate all events in database" {
            val obj = clazz.getDeclaredConstructor().newInstance()
            val method = clazz.getDeclaredMethod(
                    "handleRequest",
                    String::class.java,
                    String::class.java,
                    Int::class.java,
                    Float::class.java,
                    Float::class.java
            )
            method.invoke(obj, "hannes", "male", 32, 180, 23)
        }
    }
}