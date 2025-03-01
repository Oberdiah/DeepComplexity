package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MethodProcessing
import com.intellij.psi.PsiMethod
import testdata.MyTestData
import java.lang.reflect.Method
import kotlin.jvm.java

object TestUtilities {
    private val predictedArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)
    private val actualArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    fun testMethod(method: PsiMethod, methodName: String) {
        println("Processing method ${method.name}:")
        var errorMessage = ""
        var methodScore = 0.0

        val clazz = MyTestData::class.java
        val reflectMethod = clazz.declaredMethods.find { it.name == methodName }
            ?: throw NoSuchMethodException("Method $methodName not found in class ${clazz.name}")

        var range: IMoldableSet<*>? = null
        try {
            val returnKey = Context.Key.ReturnKey(method)

            errorMessage = "Failed to get method context"
            val context = MethodProcessing.getMethodContext(method)
            println(context.debugKey(returnKey).prependIndent())

            errorMessage = "Failed to evaluate return value range"

            range = context.evaluateKey(returnKey)
            println("\tRange of return value: $range")
            errorMessage = "Failed to verify method"
        } catch (e: Throwable) {
            println("\tAww no :( ($errorMessage)\n")
            e.printStackTrace()

            methodScore = 0.0
        }

        try {
            range?.let { methodScore = getMethodScore(reflectMethod, range) }
        } catch (e: Throwable) {
            println("\tAww no! :( ($errorMessage)\n")
            e.printStackTrace()
            methodScore = 0.0
        }

        val annotation = reflectMethod.getAnnotation(RequiredScore::class.java)
        if (annotation != null) {
            val requiredScore = annotation.value

            val methodScoreStr = String.format("%.2f", methodScore * 100)
            val requiredScoreStr = String.format("%.2f", requiredScore * 100)

            if (methodScoreStr.toDouble() < requiredScoreStr.toDouble()) {
                throw AssertionError(
                    "Method ${reflectMethod.name} failed with a score of $methodScore, " +
                            "which is less than the required score of $requiredScore"
                )
            } else {
                println(" which was sufficient (>=$requiredScoreStr%)")
            }
        } else {
            println("\n\tThis method was not required to reach a score threshold and as such it passed by default.")
        }

    }

    fun getMethodScore(method: Method, range: IMoldableSet<*>): Double {
        // For now, int-only. Short is the goal though.
        if (method.parameterCount != 1 ||
            method.parameterTypes[0] != Short::class.java ||
            method.returnType != Short::class.java
        ) {
            print("\tSkipped method ${method.name} as it's not valid for testing.")
            return 0.0
        }
        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            predictedArray[s - Short.MIN_VALUE] = range.contains(s.toShort())
            actualArray[s - Short.MIN_VALUE] = false
        }

        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            val result = method.invoke(null, s.toShort()) as Short

            if (!range.contains(result)) {
                throw AssertionError(
                    "Method ${method.name} failed for input $s," +
                            " returned a value of $result which is not in range ${range}"
                )
            } else {
                if (result !in Short.MIN_VALUE..Short.MAX_VALUE) {
                    throw AssertionError(
                        "Method ${method.name} failed for input $s," +
                                " returned a value of $result which is not in range ${Short.MIN_VALUE}..${Short.MAX_VALUE}"
                    )
                }
                actualArray[result - Short.MIN_VALUE] = true
            }
        }

        val numEntriesActual = actualArray.count { it }
        val numEntriesPredicted = predictedArray.count { it }

        val printLen = 50
        print("\n\t")
        for (i in 1..printLen) {
            val index = i - printLen / 2
            print(if (index in 0..9) "$index" else ".")
        }
        print("\n\t")
        for (i in 1..printLen) {
            val index = i - Short.MIN_VALUE - printLen / 2
            print(if (actualArray[index]) "X" else " ")
        }
        if (numEntriesActual < 20) {
            print("(" + actualArray.indices.filter { actualArray[it] }
                .joinToString(", ") { (it + Short.MIN_VALUE).toString() } + ")")
        }
        print("\n\t")
        for (i in 1..printLen) {
            val index = i - Short.MIN_VALUE - printLen / 2
            print(if (predictedArray[index]) "X" else " ")
        }
        if (numEntriesPredicted < 20) {
            print("(" + predictedArray.indices.filter { predictedArray[it] }
                .joinToString(", ") { (it + Short.MIN_VALUE).toString() } + ")")
        }
        println()
        println()

        if (numEntriesPredicted == 0) {
            return 0.0
        }

        var numEntriesCorrect = 0
        for (i in 0 until predictedArray.size) {
            if (predictedArray[i] && actualArray[i]) {
                numEntriesCorrect++
            }
        }

        val scoreFraction = numEntriesCorrect.toDouble() / numEntriesPredicted

        if (scoreFraction != 1.0) {
            // Print out all values if there are few enough.
            val filtered = predictedArray.indices.filter { predictedArray[it] && !actualArray[it] }
            val joined = filtered.take(20).joinToString(", ") { (it + Short.MIN_VALUE).toString() }
            print("\tLost score for: $joined")
            if (filtered.size > 20) {
                println(" and ${filtered.size - 20} more")
            } else {
                println()
            }
        }

        print(
            "\tReceived a score of $numEntriesCorrect/$numEntriesPredicted (${
                String.format(
                    "%.2f",
                    scoreFraction * 100
                )
            }%)"
        )

        return scoreFraction
    }
}