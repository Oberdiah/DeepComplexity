package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import testdata.MyTestData

object TestUtilities {
    private val boolArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    fun verifyMethod(methodName: String, range: IMoldableSet) {
        val clazz = MyTestData::class.java
        try {
            val method = clazz.declaredMethods.find { it.name == methodName }
                ?: throw NoSuchMethodException("Method $methodName not found in class ${clazz.name}")

            if (method.parameterCount == 1
            // method.parameterTypes[1] == Short::class.java &&
            // method.returnType == Short::class.java
            ) {
                // Clean to 0
                for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
                    boolArray[s - Short.MIN_VALUE] = range.contains(s)
                }

                val highestScore = boolArray.count { it }

                for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
                    val result = method.invoke(null, s) as Int

                    if (!range.contains(result)) {
                        throw AssertionError(
                            "Method $methodName failed for input $s," +
                                    " returned a value of $result which is not in range $range"
                        )
                    } else {
                        boolArray[result - Short.MIN_VALUE] = false
                    }
                }

                val score = highestScore - boolArray.count { it }

                println("\tPassed with a score of $score/$highestScore (${score.toDouble() / highestScore * 100}%)")
            }

        } catch (e: Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}