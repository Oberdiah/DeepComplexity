package com.github.oberdiah.deepcomplexity.staticAnalysis

import org.jetbrains.uast.UMethod

object MethodProcessing {
    fun processMethod(method: UMethod) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.

        // In order to do this, I think we want to build an expression tree for all variables.
        // A method is a thing that converts some inputs to some outputs, by way of these expressions.

        // A variable can be a return value, an input value, a local variable, anything that allows data to flow
        // through it. We'll call these 'moldables'

        val inputs = getMethodInputs(method)
        val outputs = getMethodOutputs(method)

        // Search the methods and figure out how these inputs relate to these outputs.
    }

    fun getMethodInputs(method: UMethod): List<Moldable> {
        return listOf()
    }

    fun getMethodOutputs(method: UMethod): List<Moldable> {
        return listOf()
    }
}