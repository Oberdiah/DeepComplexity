package com.oberdiah.deepcomplexity

import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.evaluation.MethodProcessing
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.ShortIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into
import com.oberdiah.deepcomplexity.utilities.Utilities
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

object TestUtilities {
    private val predictedArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)
    private val actualArray = BooleanArray(Short.MAX_VALUE.toInt() - Short.MIN_VALUE.toInt() + 1)

    data class MethodAnnotationInfo(
        val filePath: String,
        val methodName: String,
        val scoreAchieved: Double,
        val expressionSize: Int
    )

    sealed interface MethodScoreResults {
        val score: Double
        val expressionSize: Int

        val scoreReceivedColumn: String
        val extraInfoColumn: String
    }

    data class MethodFailed(val failureMessage: String) : MethodScoreResults {
        override val score: Double = 0.0
        override val expressionSize: Int = 0
        override val scoreReceivedColumn: String = "N/A"
        override val extraInfoColumn: String = failureMessage
    }

    data class MethodRan(val fraction: String, override val score: Double, override val expressionSize: Int) :
        MethodScoreResults {
        override val scoreReceivedColumn: String = fraction
        override val extraInfoColumn: String = ""
    }

    val annotationInformation = mutableListOf<MethodAnnotationInfo>()

    /**
     * Returns a string summary of the test results, to be printed in the summary test,
     * and if the test passed.
     */
    fun testMethod(testInfo: SimpleMustPassTest.TestInfo): Pair<String, Boolean> {
        Utilities.TEST_GLOBALS.SHOULD_CLONE_CONTEXTS = testInfo.testSettings.cloneContexts

        println("Processing method ${testInfo.className}:")

        val clazz = Class.forName(testInfo.psiMethod.containingClass?.qualifiedName)
        val reflectMethod = clazz.declaredMethods.find { it.name == testInfo.className }
            ?: throw NoSuchMethodException("Method ${testInfo.className} not found in class ${clazz.name}")

        val scoreResults = getMethodScore(reflectMethod, testInfo)

        // Track perfect-score methods for optional annotation updates in the summary phase.
        val filePath = testInfo
            .psiMethod
            .containingFile
            ?.virtualFile
            ?.path
            ?.replace("/src", "src/test/java/testdata")
        if (filePath != null && testInfo.testSettings.updateAnnotations) {
            annotationInformation.add(
                MethodAnnotationInfo(
                    filePath,
                    testInfo.className,
                    scoreResults.score,
                    scoreResults.expressionSize
                )
            )
        }

        val (columns, passed) = getSummaryTableRow(testInfo, scoreResults, reflectMethod)
        val columnSpacing = listOf(17, 11, 7, 1, 1)
        val summary = columns.mapIndexed { i, s -> s.padEnd(columnSpacing[i]) }.joinToString(" | ")
        return summary to passed
    }

    private fun getSummaryTableRow(
        testInfo: SimpleMustPassTest.TestInfo,
        scoreResults: MethodScoreResults,
        method: Method
    ): Pair<List<String>, Boolean> {
        val requiredScoreAnnotation = method.getAnnotation(RequiredScore::class.java)
        val expressionSizeAnnotation = method.getAnnotation(ExpectedExpressionSize::class.java)

        val scoreReceivedColumn = scoreResults.scoreReceivedColumn
        val extraInfoColumn = scoreResults.extraInfoColumn
        val methodScoreStr = String.format("%.2f", scoreResults.score * 100)
        val msg = when (scoreResults) {
            is MethodFailed -> scoreResults.failureMessage
            is MethodRan -> scoreResults.fraction
        }
        println("\tExpression size: ${scoreResults.expressionSize}")

        if (!testInfo.testSettings.ignoreExpressionSize) {
            if (expressionSizeAnnotation != null) {
                val requiredMaxExpressionSize = expressionSizeAnnotation.value
                if (scoreResults.expressionSize > requiredMaxExpressionSize) {
                    println("\tExpression size ${scoreResults.expressionSize} exceeded the expected maximum of $requiredMaxExpressionSize")
                    return listOf(
                        "### Failed ###",
                        scoreReceivedColumn,
                        "$methodScoreStr%",
                        "",
                        "Expression size ${scoreResults.expressionSize} exceeded the expected maximum of $requiredMaxExpressionSize"
                    ) to false
                }
            }
        }

        if (requiredScoreAnnotation != null) {
            val requiredScore = requiredScoreAnnotation.value
            val requiredScoreStr = String.format("%.2f", requiredScore * 100)

            if (methodScoreStr.toDouble() < requiredScoreStr.toDouble()) {
                if (scoreResults.score == 0.0) {
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
                    if (method.getAnnotation(GoodEnough::class.java) != null || methodScoreStr == "100.00") "Y" else ""

                return listOf("Passed", msg, "$methodScoreStr%", goldStar, notes) to true
            }
        } else {
            println(if (scoreResults.score == 0.0) "\t$extraInfoColumn" else "\tReceived a score of $msg ($methodScoreStr%)")
            println("\tThis method was not required to reach a score threshold and as such it passed by default.")
            return listOf("Passed by default", scoreReceivedColumn, "$methodScoreStr%", "", extraInfoColumn) to true
        }
    }

    private fun getMethodScore(method: Method, testInfo: SimpleMustPassTest.TestInfo): MethodScoreResults {
        val returnValue = try {
            MethodProcessing.getMethodContext(testInfo.psiMethod)
        } catch (e: Throwable) {
            // If it's an assertion error, we should fully error out regardless.
            // Those shouldn't be thrown, even on tests that aren't required to pass yet.
            if (e is AssertionError) {
                throw e
            }

            e.printStackTrace()
            return MethodFailed(
                (e.message ?: "Failed to parse PSI")
                    .replace("An operation is not implemented: ", ""),
            )
        }.returnValue!!.optimise()

        val range = try {
            val bundle: Bundle<*> = returnValue.evaluate(ExprEvaluate.Scope())
            // Must come after the `evaluate` call.
            println((returnValue.dStr()).prependIndent())

            // Good to calculate this after we've done our debug printing, just so if this ends up throwing
            // we still get to see the expression tree.
            val unknownsInReturn = returnValue.iterateTree<VariableExpr<*>>()
                .map { it.key }.toSet()

            // For every test we have, there is no reason for unknowns to be present by the time we return.
            // (Aside from `x`, of course, hence the `size <= 1` check.)
            assert(unknownsInReturn.size <= 1) {
                "Method '${method.name}' has unknowns in return value: ${unknownsInReturn.joinToString(", ")}"
            }

            val castBundle = bundle.cast(ShortIndicator)!!
            val collapsedBundle = castBundle.collapse().into()
            collapsedBundle
        } catch (e: Throwable) {
            e.printStackTrace()
            return MethodFailed(
                (e.message ?: "Failed to parse PSI")
                    .replace("An operation is not implemented: ", "")
            )
        }

        println("\tRange of return value: $range")

        // For now, int-only. Short is the goal though.
        if (method.parameterCount != 1 ||
            method.parameterTypes[0] != Short::class.java ||
            method.returnType != Short::class.java
        ) {
            return MethodFailed("Skipped method ${method.name} as it's not valid for testing.")
        }
        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            predictedArray[s - Short.MIN_VALUE] = range.contains(s.toShort())
            actualArray[s - Short.MIN_VALUE] = false
        }

        for (s in Short.MIN_VALUE..Short.MAX_VALUE) {
            try {
                val result = method.invoke(null, s.toShort()) as Short

                if (!range.contains(result)) {
                    return MethodFailed("Failed for input $s, returned $result which is not in range $range")
                } else {
                    if (result !in Short.MIN_VALUE..Short.MAX_VALUE) {
                        return MethodFailed("Failed for input $s, returned $result which is not in range ${Short.MIN_VALUE}..${Short.MAX_VALUE}")
                    }
                    actualArray[result - Short.MIN_VALUE] = true
                }
            } catch (e: InvocationTargetException) {
                if (e.targetException is ArithmeticException &&
                    e.targetException.message?.contains("by zero") == true
                ) {
                    if (range.hasThrownDivideByZero) {
                        // If the exception is an arithmetic exception for division by zero,
                        // and we expected that, we can ignore it.
                        continue
                    }
                }
                return MethodFailed("Failed for input $s, exception: ${e.targetException.message}")
            } catch (e: Throwable) {
                e.printStackTrace()
                return MethodFailed("Method ${method.name} threw an unexpected exception for input $s: ${e.message}")
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
            return MethodFailed("Didn't predict any valid values at all")
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

        val expressionSize = returnValue.iterateTree().map { 1 }.sum()

        return MethodRan("$numEntriesCorrect/$numEntriesPredicted", scoreFraction, expressionSize)
    }
}