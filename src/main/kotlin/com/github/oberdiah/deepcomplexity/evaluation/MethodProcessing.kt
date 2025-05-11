package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.solver.LoopSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.orElse
import com.github.oberdiah.deepcomplexity.utilities.Utilities.resolveIfNeeded
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
        println(context.toString())
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
                            val lhs = context.getVar(resolvedLhs).castToNumbers()
                            val rhs = buildExpressionFromPsi(rExpression, context).castToNumbers()

                            ConversionsAndPromotion.castNumbersAToB(rhs, lhs, false).map { rhs, lhs ->
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
                ).tryCastTo(BooleanSetIndicator) ?: TODO("Failed to cast to BooleanSet: ${psi.condition?.text}")

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = Context()
                val falseBranchContext = Context()

                processPsiElement(trueBranch, trueBranchContext)
                psi.elseBranch?.let { processPsiElement(it, falseBranchContext) }

                val ifContext = Context.combine(trueBranchContext, falseBranchContext) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                context.stack(ifContext)
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
                    buildExpressionFromPsi(condition, bodyContext).tryCastTo(BooleanSetIndicator)
                        ?: TODO("Failed to cast to BooleanSet: ${condition.text}")
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext, conditionExpr)

                context.stack(bodyContext)
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
            }

            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnExpr = buildExpressionFromPsi(returnExpression, context)

                    if (!context.isReturning()) {
                        context.assignVar(psi, returnExpr)
                    } else {
                        context.resolveVar(psi, returnExpr)
                    }
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

            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                return when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        val operandPrecast = buildExpressionFromPsi(operand, context).castToNumbers()
                        val promoted = ConversionsAndPromotion.unaryNumericPromotion(operandPrecast)

                        unaryOp.applyToExpr(promoted)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val resolvedOperand = operand.resolveIfNeeded()
                        val operandExpr = context.getVar(resolvedOperand).castToNumbers()
                        context.assignVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                        // Build the expression after the assignment for a prefix increment/decrement
                        buildExpressionFromPsi(operand, context).castToNumbers()
                    }
                }
            }

            is PsiPostfixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")


                // Build the expression before the assignment for a postfix increment/decrement
                val builtExpr = buildExpressionFromPsi(psi.operand, context).castToNumbers()

                // Assign the value to the variable
                val resolvedOperand = psi.operand.resolveIfNeeded()
                val operandExpr = context.getVar(resolvedOperand).castToNumbers()
                context.assignVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                return builtExpr
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                val comparisonOp = ComparisonOp.fromJavaTokenType(tokenType)
                val binaryNumberOp = BinaryNumberOp.fromJavaTokenType(tokenType)
                val booleanOp = BooleanOp.fromJavaTokenType(tokenType)

                if (booleanOp != null) {
                    val lhs = buildExpressionFromPsi(lhsOperand, context).tryCastTo(BooleanSetIndicator)
                        ?: throw IllegalArgumentException("Failed to cast to Boolean: ${lhsOperand.text}")
                    val rhs = buildExpressionFromPsi(rhsOperand, context).tryCastTo(BooleanSetIndicator)
                        ?: throw IllegalArgumentException("Failed to cast to Boolean: ${rhsOperand.text}")

                    return BooleanExpression(lhs, rhs, booleanOp)
                } else {
                    val lhsPrecast = buildExpressionFromPsi(lhsOperand, context).castToNumbers()
                    val rhsPrecast = buildExpressionFromPsi(rhsOperand, context).castToNumbers()

                    return ConversionsAndPromotion.binaryNumericPromotion(lhsPrecast, rhsPrecast)
                        .map { lhs, rhs ->
                            return@map when {
                                comparisonOp != null -> ComparisonExpression(lhs, rhs, comparisonOp)
                                binaryNumberOp != null -> ArithmeticExpression(lhs, rhs, binaryNumberOp)
                                else -> TODO("Unsupported binary operation: ${psi.operationSign} (${psi.text})")
                            }
                        }
                }
            }

            is PsiTypeCastExpression -> {
                val expr = buildExpressionFromPsi(psi.operand ?: throw ExpressionIncompleteException(), context)

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val type = Utilities.psiTypeToKClass(psiType.type) ?: throw IllegalArgumentException(
                    "Failed to convert PsiType to KClass: ${psiType.text}"
                )

                val setInd = SetIndicator.Companion.fromClass(type)

                return TypeCastExpression(expr, setInd, false)
            }

            is PsiParenthesizedExpression -> {
                return buildExpressionFromPsi(psi.expression ?: throw ExpressionIncompleteException(), context)
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class} (${psi.text})")
    }
}