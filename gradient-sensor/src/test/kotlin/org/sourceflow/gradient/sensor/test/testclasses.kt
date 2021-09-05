package org.sourceflow.gradient.sensor.test

import org.sourceflow.gradient.annotation.InModelingUniverse
import org.sourceflow.gradient.annotation.NotInModelingUniverse
import org.sourceflow.gradient.sensor.monitoring.ByteCodeFacade

@InModelingUniverse
class CallerClass(private val callee: CalleeClass) {

    fun methodA(fieldA: Int): Int {
        return callee.methodA(fieldA) + methodB(fieldA)
    }

    private fun methodB(a: Int): Int {
        return a + 2
    }

    fun methodC(): Int {
        return callee.fieldA + callee.fieldB
    }

    fun methodD(a: Int) {
        callee.fieldA = a
    }

    fun methodE(path: String) {
        System.err.println(path)
    }

    @NotInModelingUniverse
    fun asm() {
        ByteCodeFacade.read(callee.fieldA, 127, 128, 0L)
    }
}

class CalleeClass(
        var fieldA: Int,
        @InModelingUniverse val fieldB: Int
) {
    @InModelingUniverse
    fun methodA(a: Int): Int {
        return a + 1
    }
}
