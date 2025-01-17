package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import testdata.MyTestData

object TestUtilities {
    fun verifyMethod(methodName: String, range: IMoldableSet) {
        val clazz = MyTestData::class.java
        try {
            val method = clazz.declaredMethods.find { it.name == methodName }
                ?: throw NoSuchMethodException("Method $methodName not found in class ${clazz.name}")

            if (method.parameterCount == 1) {
                // Build arguments based on parameter types
                val args = Array<Any?>(method.parameterCount) { i ->
                    when (method.parameterTypes[i]) {
                        Short::class.javaPrimitiveType -> false
                        Int::class.javaPrimitiveType -> 0
                        else -> {
                            throw IllegalArgumentException("Unsupported parameter type: ${method.parameterTypes[i]}")
                        }
                    }
                }

                for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
                    val result = method.invoke(null, s)
                    assert(range.contains(result))
                }
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}