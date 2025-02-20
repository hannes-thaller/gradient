package org.sourceflow.gradient.project.entity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import javax.naming.Name

class EntityTest : StringSpec({
    "should create canonical name"{
        val sut = CanonicalName.from(
            "org" to NameComponentType.GROUP,
            "sourceflow" to NameComponentType.GROUP,
            "gradient" to NameComponentType.ARTIFACT,
            "Analyzer" to NameComponentType.TYPE
        )

        sut.digest() shouldBe "org.sourceflow.gradient.Analyzer"
    }
})