package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.loopEvaluation.LoopEvaluation
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.orElse
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.resolveIfNeeded
import com.intellij.psi.*

object MethodProcessing {
    fun printMethod(method: PsiMethod, evaluate: Boolean) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.
        // What makes method processing nice is we don't need to think about
        // anything outside this method. Things coming in from outside are just unknowns
        // we parameterize over.

        val context = Context()
        method.body?.let { body ->
            processPsiElement(body, context)
        }
        println(context.convertToString(evaluate))
    }

    fun getMethodContext(method: PsiMethod): Context {
        val context = Context()
        method.body?.let { body ->
            processPsiElement(body, context)
        }
        return context
    }

    private fun processPsiElement(
        psi: PsiElement,
        context: Context
    ) {
        when (psi) {
            is PsiBlockStatement -> {
                processPsiElement(psi.codeBlock, context)
            }

            is PsiCodeBlock -> {
                for (line in psi.children) {
                    processPsiElement(line, context)
                }
            }

            is PsiExpressionStatement -> {
                processPsiElement(psi.expression, context)
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                psi.declaredElements.forEach { element ->
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
                psi.rExpression?.let { rExpression ->
                    val opSign = psi.operationSign
                    when (opSign.tokenType) {
                        JavaTokenType.EQ -> {
                            context.assignVar(
                                psi.lExpression.resolveIfNeeded(),
                                buildExpressionFromPsi(rExpression, context)
                            )
                        }

                        JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                            val resolvedLhs = psi.lExpression.resolveIfNeeded()
                            val lhs = context.getVar(resolvedLhs).tryCast<NumberSet>() ?: TODO(
                                "Failed to cast to NumberSet: ${psi.lExpression.text} while parsing ${psi.text}"
                            )

                            val rhs = buildExpressionFromPsi(rExpression, context).tryCast<NumberSet>() ?: TODO(
                                "Failed to cast to NumberSet: ${rExpression.text} while parsing ${psi.text}"
                            )

                            context.assignVar(
                                resolvedLhs,
                                ArithmeticExpression(
                                    lhs,
                                    rhs,
                                    BinaryNumberOp.fromJavaTokenType(psi.operationSign.tokenType)
                                        ?: throw IllegalArgumentException(
                                            "As-yet unsupported assignment operation: ${psi.operationSign}"
                                        )
                                )
                            )
                        }

                        else -> {
                            TODO(
                                "As-yet unsupported assignment operation: ${psi.operationSign}"
                            )
                        }
                    }
                }
            }

            is PsiIfStatement -> {
                val condition = buildExpressionFromPsi(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                ).tryCast<BooleanSet>() ?: TODO("Failed to cast to BooleanSet: ${psi.condition?.text}")

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = Context()
                val falseBranchContext = Context()

                processPsiElement(trueBranch, trueBranchContext)
                psi.elseBranch?.let { processPsiElement(it, falseBranchContext) }

                context.stack(
                    Context.combine(trueBranchContext, falseBranchContext) { a, b ->
                        IfExpression.new(a, b, condition)
                    }
                )
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    processPsiElement(initialization, context)
                }

                val bodyContext = Context()

                psi.body?.let { processPsiElement(it, bodyContext) }
                psi.update?.let { processPsiElement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    buildExpressionFromPsi(condition, bodyContext).tryCast<BooleanSet>()
                        ?: TODO("Failed to cast to BooleanSet: ${condition.text}")
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopEvaluation.processLoopContext(bodyContext, conditionExpr)

                context.stack(bodyContext)
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
            }

            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnExpr = buildExpressionFromPsi(returnExpression, context)
                    context.assignVar(psi, returnExpr)
                }
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
            }

            else -> {
                println("WARN: Unsupported PsiElement type: ${psi::class} (${psi.text})")
            }
        }
    }

    /**
     * Builds up the expression tree.
     *
     * Nothing in here should be declaring variables.
     */
    private fun buildExpressionFromPsi(psi: PsiExpression, context: Context): IExpr<*> {
        when (psi) {
            is PsiLiteralExpression -> {
                val value = psi.value ?: throw ExpressionIncompleteException()
                return ConstantExpression.fromAny(value)
            }

            is PsiReferenceExpression -> {
                return context.getVar(psi.resolveIfNeeded())
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val lhs = buildExpressionFromPsi(lhsOperand, context).tryCast<NumberSet>()
                    ?: TODO("Failed to cast to NumberSet: ${lhsOperand.text} while parsing ${psi.text}")

                val rhs = buildExpressionFromPsi(rhsOperand, context).tryCast<NumberSet>()
                    ?: TODO("Failed to cast to NumberSet: ${rhsOperand.text} while parsing ${psi.text}")

                val tokenType = psi.operationSign.tokenType

                val comparisonOp = ComparisonOp.fromJavaTokenType(tokenType)
                val binaryNumberOp = BinaryNumberOp.fromJavaTokenType(tokenType)

                return when {
                    comparisonOp != null -> ComparisonExpression(lhs, rhs, comparisonOp)
                    binaryNumberOp != null -> ArithmeticExpression(lhs, rhs, binaryNumberOp)
                    else -> TODO("Unsupported binary operation: ${psi.operationSign} (${psi.text})")
                }
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class} (${psi.text})")
    }
}