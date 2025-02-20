package org.sourceflow.gradient.sensor

import io.kotest.assertions.inspecting
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.File
import java.io.IOException

class ConfigurationTest : StringSpec({
    "should load configuration"{
        val result = Configuration.load(File("src/test/resources/gradient.yaml"))
        inspecting(result) {
            group shouldBe "org.sourceflow"
            artifact shouldBe "org.sourceflow.gradinet.testing.nutrition-advisor"
        }
    }

    shouldThrow<IOException> {
        Configuration.load(File("not existing.yaml"))
    }
})