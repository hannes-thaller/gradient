package org.sourceflow.gradient.sensor

import kotlinx.coroutines.runBlocking
import java.io.File
import java.lang.instrument.ClassFileTransformer
import java.lang.instrument.Instrumentation
import java.security.ProtectionDomain


@Suppress("unused")
object Bootstrap {
    /**
     * >>
     */
    @JvmStatic
    fun premain(premainArguments: String?, instrumentation: Instrumentation) = runBlocking {
        if (premainArguments != null) {
            val configuration = Configuration.load(File(".", premainArguments))
            DIContainer.gradientAgent.run(configuration, instrumentation)
        } else {
            System.err.println("Gradient capabilities deactivated: No gradient file given.")
        }
    }
}