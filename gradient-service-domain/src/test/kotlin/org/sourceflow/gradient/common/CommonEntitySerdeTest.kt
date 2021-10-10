package org.sourceflow.gradient.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sourceflow.gradient.common.entities.CommonEntities
import java.util.*

class CommonEntitySerdeTest : StringSpec({

    "should serde UUID"{

        val id = UUID.randomUUID()
        val transformedId = CommonEntitySerde.fromUUID(id)
        val result = CommonEntitySerde.toUUID(transformedId)

        result shouldBe id
    }
})

