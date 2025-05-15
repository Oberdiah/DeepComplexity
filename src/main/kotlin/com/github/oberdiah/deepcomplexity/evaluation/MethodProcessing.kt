package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.solver.LoopSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.mapLeft
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

        val context = Context.new()
        method.body?.let { body ->
            processPsiElement(body, context)
        }
        println(context.toString())
    }

    fun getMethodContext(method: PsiMethod): Context {
        method.body?.let { body ->
            return processPsiElement(body, Context.new())
        }
        return Context.new()
    }

    private fun processPsiElement(
        psi: PsiElement,
        context: Context
    ): Context {
        when (psi) {
            is PsiBlockStatement -> {
                return processPsiElement(psi.codeBlock, context)
            }

            is PsiCodeBlock -> {
                var context = context
                for (line in psi.children) {
                    context = processPsiElement(line, context)
                }
                return context
            }

            is PsiExpressionStatement -> {
                return processPsiElement(psi.expression, context)
            }

            is PsiDeclarationStatement -> {
                var context = context
                // Handled similar to assignment expression
                psi.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            val (rhs, newContext) = buildExpressionFromPsi(rExpression, context)
                            context = newContext.withVar(element, rhs)
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
                return context
            }

            is PsiAssignmentExpression -> {
                psi.rExpression?.let { rExpression ->
                    val opSign = psi.operationSign
                    when (opSign.tokenType) {
                        JavaTokenType.EQ -> {
                            val (rhs, newContext) = buildExpressionFromPsi(rExpression, context)
                            return newContext.withVar(psi.lExpression.resolveIfNeeded(), rhs)
                        }

                        JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                            val resolvedLhs = psi.lExpression.resolveIfNeeded()
                            val lhs = context.getVar(resolvedLhs).castToNumbers()
                            val (rhs, context) = buildExpressionFromPsi(
                                rExpression,
                                context
                            ).mapLeft { it.castToNumbers() }

                            return ConversionsAndPromotion.castNumbersAToB(rhs, lhs, false).map { rhs, lhs ->
                                context.withVar(
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
                return context
            }

            is PsiIfStatement -> {
                val (condition, newContext) = buildExpressionFromPsi(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                ).mapLeft {
                    it.tryCastTo(BooleanSetIndicator)
                        ?: TODO("Failed to cast to BooleanSet: ${psi.condition?.text}")
                }

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = processPsiElement(trueBranch, Context.new())
                val falseBranchContext = psi.elseBranch?.let { processPsiElement(it, Context.new()) } ?: Context.new()

                val ifContext = Context.combine(trueBranchContext, falseBranchContext) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                return newContext.stack(ifContext)
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    processPsiElement(initialization, context)
                }

                var bodyContext = Context.new()

                psi.body?.let { processPsiElement(it, bodyContext) }
                psi.update?.let { processPsiElement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    // Unsure what to do with the context here — should it affect the body context?
                    val (boolExpr, context) = buildExpressionFromPsi(condition, bodyContext).mapLeft {
                        it.tryCastTo(BooleanSetIndicator)
                            ?: TODO("Failed to cast to BooleanSet: ${condition.text}")
                    }
                    bodyContext = context
                    boolExpr
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext, conditionExpr)

                return context.stack(bodyContext)
            }


            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val (returnExpr, newContext) = buildExpressionFromPsi(returnExpression, context)

                    return if (newContext.variables.keys.none { it.isMethod() }) {
                        // If there's no 'method' key yet, create one.
                        newContext.withVar(psi, returnExpr)
                    } else {
                        // If there is, resolve the existing one with the new value.
                        newContext.resolveVar(psi, returnExpr)
                    }
                }
                return context
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
                return context
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
                return context
            }

            is PsiExpression -> {
                // It's not assigned to anything, so we ignore the result.
                return buildExpressionFromPsi(psi, context).second
            }

            else -> {
                println("WARN: Unsupported PsiElement type: ${psi::class} (${psi.text})")
                return context
            }
        }
    }

    /**
     * Builds up the expression tree.
     *
     * Nothing in here should be declaring variables.
     */
    private fun buildExpressionFromPsi(psi: PsiExpression, context: Context): Pair<IExpr<*>, Context> {
        when (psi) {
            is PsiLiteralExpression -> {
                val value = psi.value ?: throw ExpressionIncompleteException()
                return ConstantExpression.fromAny(value) to context
            }

            is PsiReferenceExpression -> {
                return context.getVar(psi.resolveIfNeeded()) to context
            }

            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                return when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        val (operand, newContext) = buildExpressionFromPsi(operand, context).mapLeft {
                            ConversionsAndPromotion.unaryNumericPromotion(it.castToNumbers())
                        }

                        unaryOp.applyToExpr(operand) to newContext
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val resolvedOperand = operand.resolveIfNeeded()
                        val operandExpr = context.getVar(resolvedOperand).castToNumbers()

                        val contextWithOpApplied = context.withVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))
                        // Build the expression after the assignment for a prefix increment/decrement
                        val (expr, newContext) = buildExpressionFromPsi(operand, contextWithOpApplied)

                        return expr.castToNumbers() to newContext
                    }
                }
            }

            is PsiPostfixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")


                // Build the expression before the assignment for a postfix increment/decrement
                val (builtExpr, newContext) = buildExpressionFromPsi(psi.operand, context)

                // Assign the value to the variable
                val resolvedOperand = psi.operand.resolveIfNeeded()
                val operandExpr = newContext.getVar(resolvedOperand).castToNumbers()
                val contextWithOpApplied = newContext.withVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                return builtExpr.castToNumbers() to contextWithOpApplied
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                val comparisonOp = ComparisonOp.fromJavaTokenType(tokenType)
                val binaryNumberOp = BinaryNumberOp.fromJavaTokenType(tokenType)
                val booleanOp = BooleanOp.fromJavaTokenType(tokenType)

                if (booleanOp != null) {
                    val (lhsPrecast, context2) = buildExpressionFromPsi(lhsOperand, context)
                    val (rhsPrecast, context3) = buildExpressionFromPsi(rhsOperand, context2)

                    val lhs = lhsPrecast.tryCastTo(BooleanSetIndicator)
                        ?: throw IllegalArgumentException("Failed to cast to Boolean: ${lhsOperand.text}")
                    val rhs = rhsPrecast.tryCastTo(BooleanSetIndicator)
                        ?: throw IllegalArgumentException("Failed to cast to Boolean: ${rhsOperand.text}")

                    return BooleanExpression(lhs, rhs, booleanOp) to context3
                } else {
                    val (lhsPrecast, context2) = buildExpressionFromPsi(
                        lhsOperand,
                        context
                    ).mapLeft { it.castToNumbers() }

                    val (rhsPrecast, context3) = buildExpressionFromPsi(
                        rhsOperand,
                        context2
                    ).mapLeft { it.castToNumbers() }

                    return ConversionsAndPromotion.binaryNumericPromotion(lhsPrecast, rhsPrecast)
                        .map { lhs, rhs ->
                            return@map when {
                                comparisonOp != null -> ComparisonExpression(lhs, rhs, comparisonOp)
                                binaryNumberOp != null -> ArithmeticExpression(lhs, rhs, binaryNumberOp)
                                else -> TODO("Unsupported binary operation: ${psi.operationSign} (${psi.text})")
                            } to context3
                        }
                }
            }

            is PsiTypeCastExpression -> {
                val (expr, newContext) = buildExpressionFromPsi(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val type = Utilities.psiTypeToKClass(psiType.type) ?: throw IllegalArgumentException(
                    "Failed to convert PsiType to KClass: ${psiType.text}"
                )

                val setInd = SetIndicator.Companion.fromClass(type)

                return TypeCastExpression(expr, setInd, false) to newContext
            }

            is PsiParenthesizedExpression -> {
                return buildExpressionFromPsi(psi.expression ?: throw ExpressionIncompleteException(), context)
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class} (${psi.text})")
    }
}