package org.sourceflow.gradient.sensor

class TestClassLoader(
    generatedClasses: Map<String, ByteArray>,
    parent: ClassLoader = Thread.currentThread().contextClassLoader
) : ClassLoader(parent) {

    init {
        generatedClasses.forEach { (name, bytes) ->
            val cls = defineClass(name, bytes, 0, bytes.size)
            resolveClass(cls)
        }
    }
}
