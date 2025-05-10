package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.evaluation.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.MethodProcessing
import com.intellij.psi.PsiMethod
import testdata.MyTestData
import java.lang.reflect.Method

object TestUtilities {
    private val predictedArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)
    private val actualArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    /**
     * Returns a string summary of the test results, to be printed in the summary test,
     * and if the test passed.
     */
    fun testMethod(method: PsiMethod, methodName: String): Pair<String, Boolean> {
        println("Processing method ${method.name}:")

        val clazz = MyTestData::class.java
        val reflectMethod = clazz.declaredMethods.find { it.name == methodName }
            ?: throw NoSuchMethodException("Method $methodName not found in class ${clazz.name}")

        val (msg, methodScore) = getMethodScore(reflectMethod, method)

        val annotation = reflectMethod.getAnnotation(RequiredScore::class.java)

        val (columns, passed) = getSummaryTable(msg, methodScore, annotation)
        val columnSpacing = listOf(20, 25, 8, 10)

        val summary = columns.mapIndexed { i, s -> s.padEnd(columnSpacing[i]) }.joinToString(" | ")
        return summary to passed
    }

    private fun getSummaryTable(
        msg: String,
        methodScore: Double,
        annotation: RequiredScore?
    ): Pair<List<String>, Boolean> {
        val scoreReceivedColumn = if (methodScore == 0.0) "N/A" else msg
        val extraInfoColumn = if (methodScore == 0.0) msg else ""

        if (annotation != null) {
            val requiredScore = annotation.value
            val requiredScoreStr = String.format("%.2f", requiredScore * 100)
            val methodScoreStr = String.format("%.2f", methodScore * 100)

            if (methodScoreStr.toDouble() < requiredScoreStr.toDouble()) {
                if (methodScore == 0.0) {
                    println("\t$msg")
                } else {
                    println("\tReceived a score of $msg which was not sufficient (<$requiredScoreStr%)")
                }
                return listOf("### Failed ###", scoreReceivedColumn, "$requiredScoreStr%", extraInfoColumn) to false
            } else {
                println("\tReceived a score of $msg which was sufficient (>=$requiredScoreStr%)")
                val notes = if (methodScoreStr.toDouble() > requiredScoreStr.toDouble())
                    "Exceeded expectations" else ""
                return listOf("Passed", msg, "$requiredScoreStr%", notes) to true
            }
        } else {
            println("\tThis method was not required to reach a score threshold and as such it passed by default.")
            return listOf("Passed by default", scoreReceivedColumn, "N/A", extraInfoColumn) to true
        }
    }

    private fun getMethodScore(method: Method, psiMethod: PsiMethod): Pair<String, Double> {
        val returnKey = Context.Key.ReturnKey(psiMethod)

        val context = try {
            MethodProcessing.getMethodContext(psiMethod)
        } catch (e: Throwable) {
            e.printStackTrace()
            return "Failed to parse PSI" to 0.0
        }

        val range = try {
            println(context.debugKey(returnKey).prependIndent())
            context.evaluateKey(returnKey).cast(ShortSetIndicator)!!.collapse()
        } catch (e: Throwable) {
            e.printStackTrace()
            return "Failed to evaluate value range" to 0.0
        }

        println("\tRange of return value: $range")

        // For now, int-only. Short is the goal though.
        if (method.parameterCount != 1 ||
            method.parameterTypes[0] != Short::class.java ||
            method.returnType != Short::class.java
        ) {
            return "Skipped method ${method.name} as it's not valid for testing." to 0.0
        }
        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            predictedArray[s - Short.MIN_VALUE] = range.contains(s.toShort())
            actualArray[s - Short.MIN_VALUE] = false
        }

        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            val result = method.invoke(null, s.toShort()) as Short

            if (!range.contains(result)) {
                return "Method ${method.name} failed for input $s," +
                        " returned a value of $result which is not in range $range" to 0.0
            } else {
                if (result !in Short.MIN_VALUE..Short.MAX_VALUE) {
                    return "Method ${method.name} failed for input $s," +
                            " returned a value of $result which is not in range ${Short.MIN_VALUE}..${Short.MAX_VALUE}" to 0.0
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
            return "Didn't predict any valid values at all" to 0.0
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

        val score = "$numEntriesCorrect/$numEntriesPredicted (${
            String.format(
                "%.2f",
                scoreFraction * 100
            )
        }%)"

        return score to scoreFraction
    }
}