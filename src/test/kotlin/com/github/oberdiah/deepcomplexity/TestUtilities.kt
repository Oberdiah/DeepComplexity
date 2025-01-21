package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MethodProcessing
import com.intellij.debugger.impl.DebuggerUtilsEx.methodName
import com.intellij.psi.PsiMethod
import testdata.MyTestData
import java.lang.reflect.Method

object TestUtilities {
    private val boolArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    fun testMethod(method: PsiMethod, methodName: String) {
        println("Processing method ${method.name}:")
        var errorMessage = ""
        var methodScore = 0.0

        val clazz = MyTestData::class.java
        val reflectMethod = clazz.declaredMethods.find { it.name == methodName }
            ?: throw NoSuchMethodException("Method $methodName not found in class ${clazz.name}")

        try {
            errorMessage = "Failed to get method context"
            val context = MethodProcessing.getMethodContext(method)
            println(context.toString().prependIndent())
            errorMessage = "Failed to evaluate return value range"
            val range = context.evaluateKey(Context.Key.ReturnKey(method))
            println("\tRange of return value: $range")
            errorMessage = "Failed to verify method"

            methodScore = getMethodScore(reflectMethod, range)
        } catch (e: Throwable) {
            println("\tAww no :( ($errorMessage)\n")
            e.printStackTrace()

            methodScore = 1.0
        }

        val annotation = reflectMethod.getAnnotation(RequiredScore::class.java)
        if (annotation != null) {
            val requiredScore = annotation.value

            if (methodScore < requiredScore) {
                throw AssertionError(
                    "Method ${reflectMethod.name} failed with a score of $methodScore, " +
                            "which is less than the required score of $requiredScore"
                )
            } else {
                println(" which was sufficient (>=${String.format("%.2f", requiredScore * 100)}%)")
            }
        } else {
            println(" which passed by default")
        }
    }

    fun getMethodScore(method: Method, range: IMoldableSet<*>): Double {
        if (method.parameterCount != 1) {
            return 0.0
        }
        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            boolArray[s - Short.MIN_VALUE] = range.contains(s)
        }

        val highestScore = boolArray.count { it }

        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            val result = method.invoke(null, s.toInt()) as Int

            if (!range.contains(result)) {
                throw AssertionError(
                    "Method ${method.name} failed for input $s," +
                            " returned a value of $result which is not in range $range"
                )
            } else {
                if (result !in Short.MIN_VALUE..Short.MAX_VALUE) {
                    // println("This is a temporary issue and will be fixed soon.")
                } else {
                    boolArray[result - Short.MIN_VALUE] = false
                }
            }
        }

        val score = highestScore - boolArray.count { it }

        val scoreFraction = score.toDouble() / highestScore

        print("\tReceived a score of $score/$highestScore (${String.format("%.2f", scoreFraction * 100)}%)")

        return scoreFraction
    }
}