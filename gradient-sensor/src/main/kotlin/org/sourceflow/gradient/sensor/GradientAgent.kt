package org.sourceflow.gradient.sensor

import mu.KotlinLogging
import org.sourceflow.gradient.common.toSimpleString
import org.sourceflow.gradient.sensor.code.CodeAnalysis
import org.sourceflow.gradient.sensor.entity.CanonicalNameFactory
import org.sourceflow.gradient.sensor.monitoring.ClassFileTransformer
import org.sourceflow.gradient.sensor.persistence.CodeDao
import org.sourceflow.gradient.sensor.persistence.MonitoringDao
import org.sourceflow.gradient.sensor.persistence.ProjectDao
import java.lang.instrument.Instrumentation

private val logger = KotlinLogging.logger { }

class GradientAgent(
    private val projectDao: ProjectDao,
    private val codeDao: CodeDao,
    private val monitoringDao: MonitoringDao
) {
    suspend fun run(config: Configuration, instrumentation: Instrumentation) {
        with(config) {
            val projectContext =
                projectDao.registerProject(CanonicalNameFactory.newProjectName(group, artifact, version))
            logger.debug { "Received project context ${projectContext.toSimpleString()}" }

            val elementGraph = CodeAnalysis.analyzeClasspath(packageRegex, classRegex, universeRegex, boundaryRegex)
            logger.debug { "Constructed the element graph $elementGraph" }

            if (elementGraph.isNotEmpty()) {

                val modelElements = codeDao.reportElementGraph(projectContext, elementGraph)
                if (modelElements.isNotEmpty()) {
                    elementGraph.addCodeElementsToModelUniverse(modelElements.map { it.elementId })

                    logger.debug { "Registering class transformer" }
                    instrumentation.addTransformer(
                        ClassFileTransformer(elementGraph.inUniverseTypeNames(), elementGraph.nameMap())
                    )
                    logger.debug { "Instrumenting classes: ${elementGraph.inUniverseTypeNames()}" }

                    val classes = elementGraph.typesAndInfo()
                        .map { (info, _) -> Class.forName(info.name) }
                        .toTypedArray()
                    instrumentation.retransformClasses(*classes)

                    monitoringDao.reportOn(projectContext)
                } else {
                    logger.warn { "No connection to Gradient or no model elements found." }
                }
            } else {
                logger.warn { "No code elements included in the analysis. Check the provided regex definitions." }
            }
        }
    }
}