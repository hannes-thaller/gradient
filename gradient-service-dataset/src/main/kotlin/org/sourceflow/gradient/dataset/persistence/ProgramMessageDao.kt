package org.sourceflow.gradient.dataset.persistence

import org.sourceflow.gradient.code.entities.CodeEntities


class ProgramMessageDao(private val program: CodeEntities.ProgramDetail) {
    private val propertyIndex = createPropertyIndex(program)
    private val executableIndex = createExecutableIndex(program)
    private val parameterIndex = createParameterIndex(program)

    private fun <T> Iterable<T>.assert(
        exception: IllegalArgumentException = IllegalArgumentException("Element does not match the criteria"),
        criteria: (T) -> Boolean
    ): Iterable<T> {
        this.forEach {
            if (!criteria(it)) {
                throw exception
            }
        }
        return this
    }

    private fun createPropertyIndex(program: CodeEntities.ProgramDetail): Map<Int, CodeEntities.CodeElement> {
        return program.propertiesList
            .assert { it.hasProperty() }
            .associateBy { it.id }
    }

    private fun createExecutableIndex(program: CodeEntities.ProgramDetail): Map<Int, CodeEntities.CodeElement> {
        return program.executablesList
            .assert { it.hasExecutable() }
            .associateBy { it.id }
    }

    private fun createParameterIndex(program: CodeEntities.ProgramDetail): Map<Int, CodeEntities.CodeElement> {
        return program.parametersList
            .assert { it.hasParameter() }
            .associateBy { it.id }
    }

    fun getExecutables(): List<CodeEntities.CodeElement> {
        return program.executablesList
    }

    fun getParameters(e: CodeEntities.CodeElement): List<CodeEntities.CodeElement> {
        require(e.hasExecutable())
        return e.executable.parametersList
            .map { parameterIndex.getValue(it) }
    }

    fun getPropertyReads(e: CodeEntities.CodeElement): List<CodeEntities.CodeElement> {
        require(e.hasExecutable())
        return e.executable.readsList
            .map { propertyIndex.getValue(it) }
    }

    fun getPropertyWrites(e: CodeEntities.CodeElement): List<CodeEntities.CodeElement> {
        require(e.hasExecutable())
        return e.executable.writesList
            .map { propertyIndex.getValue(it) }
    }

    fun getInvocations(e: CodeEntities.CodeElement): List<CodeEntities.CodeElement> {
        require(e.hasExecutable())
        return e.executable.invokesList
            .map { executableIndex.getValue(it) }
    }
}