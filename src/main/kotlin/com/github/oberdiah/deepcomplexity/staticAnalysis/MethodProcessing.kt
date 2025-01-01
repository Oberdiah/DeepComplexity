package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.resolveIfNeeded
import com.intellij.psi.*

object MethodProcessing {
    fun processMethod(method: PsiMethod) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.
        // What makes method processing nice is we don't need to think about
        // anything outside this method. Things coming in from outside are just unknowns
        // we parameterize over.

        val context = Context()

        method.body?.let { body ->
//            val flow = ControlFlowFactory
//                .getControlFlow(
//                    body,
//                    AllVariablesControlFlowPolicy.getInstance(),
//                    ControlFlowOptions.create(true, true, false)
//                )

            processBody(body, context)
        }

        println(context)
    }

    private fun processBody(
        body: PsiCodeBlock,
        context: Context
    ) {
        for (line in body.children) {
            processPsiElement(line, context)
        }
    }

    private fun processPsiElement(
        line: PsiElement,
        context: Context
    ) {
        when (line) {
            is PsiExpressionStatement -> {
                processPsiElement(line.expression, context)
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                line.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            context.assignVar(
                                element,
                                buildExpressionFromPsi(rExpression, context)
                            )
                        } else {
                            TODO(
                                "Eventually we'll replace this with either null or 0, " +
                                        "for now it'll remain blank."
                            )
                        }
                    } else {
                        throw IllegalArgumentException(
                            "As-yet unsupported PsiElement type for variable declaration: $element"
                        )
                    }
                }
            }

            is PsiAssignmentExpression -> {
                line.rExpression?.let { rExpression ->
                    val opSign = line.operationSign
                    when (opSign.tokenType) {
                        JavaTokenType.EQ -> {
                            context.assignVar(
                                line.lExpression,
                                buildExpressionFromPsi(rExpression, context)
                            )
                        }

                        JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                            val resolvedLhs = line.lExpression.resolveIfNeeded()
                            val lhs = context.getVar(resolvedLhs).expression.attemptCastTo<NumberSet>() ?: TODO(
                                "Failed to cast to NumberSet: ${line.lExpression.text}"
                            )

                            val rhs = buildExpressionFromPsi(rExpression, context).attemptCastTo<NumberSet>() ?: TODO(
                                "Failed to cast to NumberSet: ${rExpression.text}"
                            )

                            context.assignVar(
                                resolvedLhs,
                                ArithmeticExpression(
                                    lhs,
                                    rhs,
                                    when (line.operationSign.tokenType) {
                                        JavaTokenType.PLUSEQ -> BinaryNumberOperation.ADDITION
                                        JavaTokenType.MINUSEQ -> BinaryNumberOperation.SUBTRACTION
                                        JavaTokenType.ASTERISKEQ -> BinaryNumberOperation.MULTIPLICATION
                                        JavaTokenType.DIVEQ -> BinaryNumberOperation.DIVISION
                                        else -> throw IllegalArgumentException(
                                            "As-yet unsupported assignment operation: ${line.operationSign}"
                                        )
                                    }
                                )
                            )
                        }

                        else -> {
                            TODO(
                                "As-yet unsupported assignment operation: ${line.operationSign}"
                            )
                        }
                    }
                }
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
            }

            is PsiIfStatement -> {
                // This is a conditional statement
            }

            is PsiReturnStatement -> {

            }

        }
    }

    /**
     * Builds up the expression tree.
     *
     * Nothing in here should be declaring variables.
     */
    private fun buildExpressionFromPsi(psi: PsiExpression, context: Context): Expression<MoldableSet> {
        when (psi) {
            is PsiLiteralExpression -> {
                val value = psi.value ?: TODO("Not implemented yet")
                return ConstantExpression.fromAny(value)
            }

            is PsiReferenceExpression -> {
                return context.getVar(psi.resolveIfNeeded()).expression
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class}")
    }
}