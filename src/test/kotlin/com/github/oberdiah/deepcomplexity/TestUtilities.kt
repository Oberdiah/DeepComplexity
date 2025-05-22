package com.github.oberdiah.deepcomplexity

import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.evaluation.MethodProcessing
import com.github.oberdiah.deepcomplexity.staticAnalysis.ShortSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.intellij.psi.PsiMethod
import java.lang.reflect.Method

object TestUtilities {
    private val predictedArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)
    private val actualArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    /**
     * Returns a string summary of the test results, to be printed in the summary test,
     * and if the test passed.
     */
    fun testMethod(method: SimpleMustPassTest.TestInfo): Pair<String, Boolean> {
        println("Processing method ${method.name}:")

        val clazz = Class.forName(method.psiMethod.containingClass?.qualifiedName)
        val reflectMethod = clazz.declaredMethods.find { it.name == method.name }
            ?: throw NoSuchMethodException("Method ${method.name} not found in class ${clazz.name}")

        val (msg, methodScore) = getMethodScore(reflectMethod, method.psiMethod)

        val annotation = reflectMethod.getAnnotation(RequiredScore::class.java)

        val (columns, passed) = getSummaryTableRow(msg, methodScore, annotation, reflectMethod)
        val columnSpacing = listOf(17, 11, 7, 1, 1)

        val summary = columns.mapIndexed { i, s -> s.padEnd(columnSpacing[i]) }.joinToString(" | ")
        return summary to passed
    }

    private fun getSummaryTableRow(
        msg: String,
        methodScore: Double,
        annotation: RequiredScore?,
        method: Method
    ): Pair<List<String>, Boolean> {
        val scoreReceivedColumn = if (methodScore == 0.0) "N/A" else msg
        val extraInfoColumn = if (methodScore == 0.0) msg else ""
        val methodScoreStr = String.format("%.2f", methodScore * 100)

        if (annotation != null) {
            val requiredScore = annotation.value
            val requiredScoreStr = String.format("%.2f", requiredScore * 100)

            if (methodScoreStr.toDouble() < requiredScoreStr.toDouble()) {
                if (methodScore == 0.0) {
                    println("\t$msg")
                } else {
                    println("\tReceived a score of $msg ($methodScoreStr%) which was not sufficient (<$requiredScoreStr%)")
                }
                return listOf("### Failed ###", scoreReceivedColumn, "$requiredScoreStr%", "", extraInfoColumn) to false
            } else {
                println("\tReceived a score of $msg ($methodScoreStr%) which was sufficient (>=$requiredScoreStr%)")
                val notes = if (methodScoreStr.toDouble() > requiredScoreStr.toDouble())
                    "Exceeded expectations" else ""

                val goldStar =
                    if (method.getAnnotation(GoodEnough::class.java) != null || methodScoreStr == "100.00") "✓" else ""

                return listOf("Passed", msg, "$methodScoreStr%", goldStar, notes) to true
            }
        } else {
            println(if (methodScore == 0.0) "\t$extraInfoColumn" else "\tReceived a score of $msg")
            println("\tThis method was not required to reach a score threshold and as such it passed by default.")
            return listOf("Passed by default", scoreReceivedColumn, "$methodScoreStr%", "", extraInfoColumn) to true
        }
    }

    private fun getMethodScore(method: Method, psiMethod: PsiMethod): Pair<String, Double> {
        val methodKey = Context.Key.MethodKey(psiMethod)

        val context = try {
            MethodProcessing.getMethodContext(psiMethod)
        } catch (e: Throwable) {
            e.printStackTrace()
            return (e.message ?: "Failed to parse PSI")
                .replace("An operation is not implemented: ", "") to 0.0
        }

        val range = try {
//            println(context.debugKey(methodKey).prependIndent())
            val bundle: Bundle<*> = context.evaluateKey(methodKey)
            bundle.cast(ShortSetIndicator)!!.collapse()
        } catch (e: Throwable) {
            e.printStackTrace()
            return (e.message ?: "Failed to parse PSI")
                .replace("An operation is not implemented: ", "") to 0.0
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
                return "Failed for input $s, returned $result which is not in range $range" to 0.0
            } else {
                if (result !in Short.MIN_VALUE..Short.MAX_VALUE) {
                    return "Failed for input $s, returned $result which is not in range ${Short.MIN_VALUE}..${Short.MAX_VALUE}" to 0.0
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

        return "$numEntriesCorrect/$numEntriesPredicted" to scoreFraction
    }
}