package org.sourceflow.gradient.sensor

import mu.KotlinLogging
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import java.io.File
import java.io.IOException

private val logger = KotlinLogging.logger {}

data class Configuration(
        var group: String = "",
        var artifact: String = "",
        var version: String = "",

        /** Whitelist packages */
        var packageRegex: List<String> = emptyList(),

        /** Whitelist classes */
        var classRegex: List<String> = emptyList(),

        /** Whitelist boundary classes */
        var boundaryRegex: List<String> = emptyList(),

        /** Whitelist universe elements */
        var universeRegex: List<String> = emptyList()
) {
    companion object {
        fun load(file: File): Configuration {
            return if (file.exists()) {
                Yaml(Constructor(Configuration::class.java))
                        .load<Configuration>(file.readText())
            } else {
                logger.warn {
                    "Gradient capabilities deactivated.\n Configuration file does not exist $file"
                }
                throw IOException()
            }
        }
    }
}