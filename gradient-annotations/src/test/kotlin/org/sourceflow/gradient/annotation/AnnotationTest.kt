package org.sourceflow.gradient.annotation

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.mpp.hasAnnotation

class AnnotationTest : StringSpec({
    "should annotate class"{
        @InModelingUniverse
        class TestClass

        TestClass::class.hasAnnotation<InModelingUniverse>().shouldBeTrue()
    }
})
