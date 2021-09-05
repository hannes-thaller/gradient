package org.sourceflow.gradient.sensor.code

import io.github.classgraph.ClassGraph
import io.github.classgraph.ClassInfo
import io.github.classgraph.FieldInfo
import io.github.classgraph.MethodInfo
import mu.KotlinLogging
import org.sourceflow.gradient.sensor.entity.CodeElementGraph
import org.sourceflow.gradient.sensor.entity.ModelingUniverseStatus

private val logger = KotlinLogging.logger { }


object CodeAnalysis {
    private const val ANNOTATION_NOT_IN_UNIVERSE = "org.sourceflow.gradient.annotation.NotInModelingUniverse"
    private const val ANNOTATION_IN_UNIVERSE = "org.sourceflow.gradient.annotation.InModelingUniverse"

    fun analyzeClasspath(
        packageRegex: List<String> = emptyList(),
        classRegex: List<String> = emptyList(),
        universeRegex: List<String> = emptyList(),
        boundaryRegex: List<String> = emptyList()
    ): CodeElementGraph {
        logger.debug {
            """Analyzing classpath with the following boundaries
                    |package=$packageRegex
                    |class=$classRegex
                    |universe=$universeRegex
                    |boundary=$boundaryRegex""".trimMargin()
        }

        return ClassGraph()
            .enableClassInfo()
            .enableFieldInfo()
            .enableMethodInfo()
            .ignoreClassVisibility()
            .ignoreFieldVisibility()
            .ignoreMethodVisibility()
            .enableExternalClasses()
            .enableInterClassDependencies()
            .enableSystemJarsAndModules()
            .whitelistPackages(*packageRegex.toTypedArray())
            .scan().use { graph ->
                logger.debug { "Analyzing classpath ${graph.classpath}" }

                val classRegexCompiled = classRegex.map { it.toRegex() }
                val universeRegexCompiled = universeRegex.map { it.toRegex() }
                val boundaryRegexCompiled = boundaryRegex.map { it.toRegex() }

                val classes = graph.allClasses
                    .filter { el -> classRegexCompiled.isEmpty() || classRegexCompiled.any { it.matches(el.name) } }
                    .filterNot { el -> el.isAnonymousInnerClass }
                val elementGraph = createCodeElements(classes, universeRegexCompiled, boundaryRegexCompiled)
                analyzeDependencies(elementGraph)
            }
    }

    private fun createCodeElements(
        classpath: List<ClassInfo>,
        universeRegex: List<Regex>,
        boundaryRegex: List<Regex>
    ): CodeElementGraph {
        val graph = CodeElementGraph()

        classpath.map { ci ->
            val type = graph.getOrPut(ci).also { type ->
                type.status = modelingStatusType(ci, type.name.toString(), universeRegex, boundaryRegex)
            }

            ci.methodAndConstructorInfo
                .filterNot(MethodInfo::isSynthetic)
                .map { mi ->
                    val executable = graph.getOrPut(ci, mi).also { ex ->
                        type.executables.add(ex)
                        ex.status =
                            modelingStatusExecutable(mi, ex.name.toString(), type.status, universeRegex, boundaryRegex)
                    }
                    mi.parameterInfo.forEachIndexed { index, mpi ->
                        graph.getOrPut(ci, mi, mpi, index).run {
                            executable.parameters.add(this)
                            status = executable.status
                        }
                    }
                }

            ci.fieldInfo.map { fi ->
                graph.getOrPut(ci, fi).let {
                    type.properties.add(it)
                    it.status =
                        modelingStatusProperty(fi, it.name.toString(), type.status, universeRegex, boundaryRegex)
                }
            }
        }

        return graph
    }

    private fun modelingStatusType(
        ci: ClassInfo,
        canonicalName: String,
        universeRegex: List<Regex>,
        boundaryRegex: List<Regex>
    ): ModelingUniverseStatus {
        return when {
            ci.getAnnotationInfo(ANNOTATION_NOT_IN_UNIVERSE) != null -> ModelingUniverseStatus.EXTERNAL
            ci.getAnnotationInfo(ANNOTATION_IN_UNIVERSE) != null -> ModelingUniverseStatus.INTERNAL
            universeRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.INTERNAL
            boundaryRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.BOUNDARY
            else -> ModelingUniverseStatus.EXTERNAL
        }
    }

    private fun modelingStatusExecutable(
        mi: MethodInfo,
        canonicalName: String,
        typeStatus: ModelingUniverseStatus,
        universeRegex: List<Regex>,
        boundaryRegex: List<Regex>
    ): ModelingUniverseStatus {
        return when {
            mi.getAnnotationInfo(ANNOTATION_NOT_IN_UNIVERSE) != null -> ModelingUniverseStatus.EXTERNAL
            mi.getAnnotationInfo(ANNOTATION_IN_UNIVERSE) != null -> ModelingUniverseStatus.INTERNAL
            universeRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.INTERNAL
            boundaryRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.BOUNDARY
            else -> typeStatus
        }
    }

    private fun modelingStatusProperty(
        fi: FieldInfo,
        canonicalName: String,
        typeStatus: ModelingUniverseStatus,
        universeRegex: List<Regex>,
        boundaryRegex: List<Regex>
    ): ModelingUniverseStatus {
        return when {
            fi.getAnnotationInfo(ANNOTATION_NOT_IN_UNIVERSE) != null -> ModelingUniverseStatus.EXTERNAL
            fi.getAnnotationInfo(ANNOTATION_IN_UNIVERSE) != null -> ModelingUniverseStatus.INTERNAL
            universeRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.INTERNAL
            boundaryRegex.any { it.matches(canonicalName) } -> ModelingUniverseStatus.BOUNDARY
            else -> typeStatus
        }
    }


    private fun analyzeDependencies(graph: CodeElementGraph): CodeElementGraph {
        graph.typesAndInfo()
            .filter { (info, type) -> !info.isExternalClass && type.status == ModelingUniverseStatus.INTERNAL }
            .forEach { (ci, _) ->
                DependencyAnalyzer.analyze(ci)
                    .groupBy { it.sourceMethod }
                    .forEach { (_, dependencies) ->
                        dependencies.forEach { dependency ->
                            if (dependency.type == DependencyAnalyzer.DependencyType.INVOKE) {
                                resolveInvokeDependency(dependency, graph)
                            } else {
                                resolveAccessDependency(dependency, graph)
                            }
                        }
                    }
            }

        return graph
    }

    private fun resolveInvokeDependency(
        dependency: DependencyAnalyzer.Dependency,
        graph: CodeElementGraph
    ) {
        val sourceMethod = dependency.sourceMethod
        val targetMethod = dependency.targetMethod
        requireNotNull(targetMethod)

        val sourceExecutable = graph.getOrPut(dependency.sourceClass, sourceMethod)
        val targetExecutable = graph.getOrPut(dependency.targetClass, targetMethod)
        if (targetExecutable !in sourceExecutable.invokes) {
            sourceExecutable.invokes.add(targetExecutable)
        }

        val sourceType = graph.getOrPut(targetMethod.classInfo)
        if (targetExecutable !in sourceType.executables) {
            sourceType.executables.add(targetExecutable)
        }

        targetMethod.parameterInfo.forEachIndexed { i, mpi ->
            val parameter = graph.getOrPut(dependency.targetClass, targetMethod, mpi, i)
            if (parameter !in targetExecutable.parameters) {
                targetExecutable.parameters.add(parameter)
            }
        }
    }

    private fun resolveAccessDependency(
        dependency: DependencyAnalyzer.Dependency,
        graph: CodeElementGraph
    ) {
        val sourceMethod = dependency.sourceMethod
        val targetMethod = dependency.targetField
        requireNotNull(targetMethod)

        val sourceExecutable = graph.getOrPut(dependency.sourceClass, sourceMethod)
        val targetProperty = graph.getOrPut(dependency.targetClass, targetMethod)

        val sourceType = graph.getOrPut(targetMethod.classInfo)
        if (targetProperty !in sourceType.properties) {
            sourceType.properties.add(targetProperty)
        }

        if (dependency.type == DependencyAnalyzer.DependencyType.READ) {
            sourceExecutable.reads.add(targetProperty)
        } else {
            sourceExecutable.writes.add(targetProperty)
        }
    }
}

