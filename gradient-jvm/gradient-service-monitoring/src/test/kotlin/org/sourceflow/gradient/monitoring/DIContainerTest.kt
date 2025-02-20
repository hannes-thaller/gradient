package org.sourceflow.gradient.monitoring

import org.sourceflow.gradient.monitoring.DIContainer
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull


class BootstrapTest : StringSpec({
    "should provide dao"{
        DIContainer.mongoSettings.shouldNotBeNull()
    }
})