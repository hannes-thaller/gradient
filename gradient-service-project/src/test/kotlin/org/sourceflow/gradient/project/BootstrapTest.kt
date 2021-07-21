package org.sourceflow.gradient.project

import io.kotest.core.spec.style.StringSpec

class BootstrapTest : StringSpec({
    "should run stub".config(enabled = false){
        main()
    }
})