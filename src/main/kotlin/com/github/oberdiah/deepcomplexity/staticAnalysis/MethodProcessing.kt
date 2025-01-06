package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.*
import com.github.oberdiah.deepcomplexity.evaluation.ArithmeticExpression.BinaryNumberOperation
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonExpression.ComparisonOperation
import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.loopEvaluation.LoopEvaluation
import com.github.oberdiah.deepcomplexity.staticAnalysis.Utilities.orElse
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
            processPsiElement(body, context)
        }
        println(context)
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
                            val lhs = context.getVar(resolvedLhs).asRetNum() ?: TODO(
                                "Failed to cast to NumberSet: ${psi.lExpression.text}"
                            )

                            val rhs = buildExpressionFromPsi(rExpression, context).asRetNum() ?: TODO(
                                "Failed to cast to NumberSet: ${rExpression.text}"
                            )

                            context.assignVar(
                                resolvedLhs,
                                ArithmeticExpression(
                                    lhs,
                                    rhs,
                                    BinaryNumberOperation.fromJavaTokenType(psi.operationSign.tokenType)
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
                ).asRetBool() ?: TODO("Failed to cast to BooleanSet: ${psi.condition?.text}")

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = Context()
                val falseBranchContext = Context()

                processPsiElement(trueBranch, trueBranchContext)
                psi.elseBranch?.let { processPsiElement(it, falseBranchContext) }

                context.stack(
                    Context.combine(trueBranchContext, falseBranchContext) { a, b ->
                        IfExpression(a, b, condition)
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
                    buildExpressionFromPsi(condition, bodyContext).asRetBool()
                        ?: throw IllegalArgumentException("Failed to cast to BooleanSet: ${condition.text}")
                }.orElse {
                    ConstantExpression.ConstExprBool(BooleanSet.fromBoolean(true))
                }

                LoopEvaluation.processLoopContext(bodyContext, conditionExpr)

                context.stack(bodyContext)
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
            }

            is PsiReturnStatement -> {

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
    private fun buildExpressionFromPsi(psi: PsiExpression, context: Context): IExpr {
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

                val lhs = buildExpressionFromPsi(lhsOperand, context).asRetNum()
                    ?: TODO("Failed to cast to NumberSet: ${lhsOperand.text}")

                val rhs = buildExpressionFromPsi(rhsOperand, context).asRetNum()
                    ?: TODO("Failed to cast to NumberSet: ${rhsOperand.text}")

                val tokenType = psi.operationSign.tokenType

                val comparisonOperation = ComparisonOperation.fromJavaTokenType(tokenType)
                val binaryNumberOperation = BinaryNumberOperation.fromJavaTokenType(tokenType)

                return when {
                    comparisonOperation != null -> ComparisonExpression(lhs, rhs, comparisonOperation)
                    binaryNumberOperation != null -> ArithmeticExpression(lhs, rhs, binaryNumberOperation)
                    else -> TODO("Unsupported binary operation: ${psi.operationSign}")
                }
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class} (${psi.text})")
    }
}