package org.sourceflow.gradient.dataset.service

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import org.sourceflow.gradient.code.entities.CodeEntities
import java.util.*

class FeatureDescriptionBuilderTest : StringSpec({

    "given empty detail, should create empty feature description"{
        val detail = CodeEntities.ProgramDetail.newBuilder()
            .build()
        val sut = FeatureDescriptionBuilder(UUID.randomUUID(), detail)

        val result = sut.build()

        result.shouldBeEmpty()
    }
})
