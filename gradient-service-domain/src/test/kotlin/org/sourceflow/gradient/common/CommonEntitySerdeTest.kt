package org.sourceflow.gradient.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.util.*

class CommonEntitySerdeTest : StringSpec({

    "should serde UUID"{
        val value = UUID.randomUUID()
        val transformedValue = CommonEntitySerde.fromUUID(value)
        val result = CommonEntitySerde.toUUID(transformedValue)

        result shouldBe value
    }

    "should serde float"{
        val value = 1.0f
        val transformedValue = CommonEntitySerde.fromFloat(value)
        val result = CommonEntitySerde.toFloat(transformedValue)

        result shouldBe value
    }
})

