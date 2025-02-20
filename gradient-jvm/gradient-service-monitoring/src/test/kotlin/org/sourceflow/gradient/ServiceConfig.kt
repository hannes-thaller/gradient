package org.sourceflow.gradient

import io.kotest.core.config.AbstractProjectConfig
import io.kotest.core.listeners.Listener
import io.kotest.extensions.junitxml.JunitXmlReporter

class ServiceConfig : AbstractProjectConfig() {
    override fun listeners(): List<Listener> = listOf(
        JunitXmlReporter(
            includeContainers = false,
            useTestPathAsName = true
        )
    )
}