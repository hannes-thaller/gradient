package org.sourceflow.gradient.code.service.entity

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.property.checkAll
import io.kotest.property.forAll
import org.sourceflow.gradient.code.service.entity.CodeEntityGenerator

class TestEntityGeneratorCode : StringSpec({
    "should generate canonical names"{
        forAll(CodeEntityGenerator.canonicalNames()) {
            it.typesCount > 0
            it.typesCount == it.componentsCount
        }
    }

    "should generate program"{
        checkAll(2, CodeEntityGenerator.programs()) {
            it.typesCount shouldBeGreaterThan 0
        }
    }
})