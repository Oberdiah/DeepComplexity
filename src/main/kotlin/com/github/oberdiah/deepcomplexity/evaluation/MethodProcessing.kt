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
import com.intellij.psi.tree.IElementType
import com.jetbrains.rd.util.firstOrNull

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
        val context = Context.new()
        method.body?.let { body ->
            processPsiElement(body, context)
        }
        return context
    }

    private fun processPsiElement(
        psi: PsiElement,
        context: Context
    ): Expr<*> {
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
                            val rhs = processPsiElement(rExpression, context)
                            context.putVar(element, rhs)
                        } else {
                            TODO("Eventually we'll replace this with either null or 0")
                        }
                    } else {
                        throw IllegalArgumentException(
                            "As-yet unsupported PsiElement type for variable declaration: $element"
                        )
                    }
                }
            }

            is PsiIfStatement -> {
                val condition = processPsiElement(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                ).castToBoolean()

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = Context.new()
                processPsiElement(trueBranch, trueBranchContext)
                val falseBranchContext = Context.new()
                psi.elseBranch?.let { processPsiElement(it, falseBranchContext) } ?: Context.new()

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

                val bodyContext = Context.new()

                psi.body?.let { processPsiElement(it, bodyContext) }
                psi.update?.let { processPsiElement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    processPsiElement(condition, bodyContext).tryCastTo(BooleanSetIndicator)
                        ?: TODO("Failed to cast to BooleanSet: ${condition.text}")
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext, conditionExpr)

                context.stack(bodyContext)
            }


            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnExpr = processPsiElement(returnExpression, context)

                    if (context.variables.keys.none { it.isMethod() }) {
                        // If there's no 'method' key yet, create one.
                        context.putVar(psi, returnExpr)
                    } else {
                        // If there is, resolve the existing one with the new value.
                        context.resolveVar(psi, returnExpr)
                    }
                }
            }

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
                        val operand =
                            ConversionsAndPromotion.unaryNumericPromotion(
                                processPsiElement(
                                    operand,
                                    context
                                ).castToNumbers()
                            )


                        unaryOp.applyToExpr(operand)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val resolvedOperand = operand.resolveIfNeeded()
                        val operandExpr = context.getVar(resolvedOperand).castToNumbers()

                        context.putVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                        // Build the expression after the assignment for a prefix increment/decrement
                        return processPsiElement(operand, context).castToNumbers()
                    }
                }
            }

            is PsiPostfixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")


                // Build the expression before the assignment for a postfix increment/decrement
                val builtExpr = processPsiElement(psi.operand, context)

                // Assign the value to the variable
                val resolvedOperand = psi.operand.resolveIfNeeded()
                val operandExpr = context.getVar(resolvedOperand).castToNumbers()
                context.putVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                return builtExpr.castToNumbers()
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                val lhs = processPsiElement(lhsOperand, context)
                val rhs = processPsiElement(rhsOperand, context)

                return processBinaryExpr(lhs, rhs, tokenType)
            }

            is PsiTypeCastExpression -> {
                val expr = processPsiElement(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val type = Utilities.psiTypeToKClass(psiType.type) ?: throw IllegalArgumentException(
                    "Failed to convert PsiType to KClass: ${psiType.text}"
                )

                val setInd = SetIndicator.Companion.fromClass(type)

                return TypeCastExpression(expr, setInd, false)
            }

            is PsiParenthesizedExpression -> {
                return processPsiElement(psi.expression ?: throw ExpressionIncompleteException(), context)
            }

            is PsiPolyadicExpression -> {
                val operands = psi.operands
                if (operands.size < 2) {
                    throw ExpressionIncompleteException()
                } else {
                    // Process it as a bunch of binary expressions in a row, left to right.
                    val tokenType = psi.operationTokenType
                    val originalLhs = processPsiElement(operands[0], context)
                    val originalRhs = processPsiElement(operands[1], context)

                    var currentExpr = processBinaryExpr(originalLhs, originalRhs, tokenType)

                    for (i in 2 until operands.size) {
                        val nextRhs = processPsiElement(operands[i], context)
                        val newExpr = processBinaryExpr(currentExpr, nextRhs, tokenType)
                        currentExpr = newExpr
                    }

                    return currentExpr
                }
            }

            is PsiAssignmentExpression -> {
                val lExpression = psi.lExpression
                val rExpression = psi.rExpression ?: throw ExpressionIncompleteException()
                val opSign = psi.operationSign

                return when (opSign.tokenType) {
                    JavaTokenType.EQ -> {
                        val rhs = processPsiElement(rExpression, context)
                        context.putVar(lExpression.resolveIfNeeded(), rhs)
                        rhs
                    }

                    JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                        val resolvedLhs = lExpression.resolveIfNeeded()
                        val lhs = context.getVar(resolvedLhs).castToNumbers()
                        val rhs = processPsiElement(
                            rExpression,
                            context
                        ).castToNumbers()

                        ConversionsAndPromotion.castNumbersAToB(rhs, lhs, false).map { rhs, lhs ->
                            val expr = ArithmeticExpression(
                                lhs,
                                rhs,
                                BinaryNumberOp.fromJavaTokenType(opSign.tokenType)
                                    ?: throw IllegalArgumentException(
                                        "As-yet unsupported assignment operation: $opSign"
                                    )
                            )
                            context.putVar(resolvedLhs, expr)
                            expr
                        }
                    }

                    else -> {
                        TODO(
                            "As-yet unsupported assignment operation: $opSign"
                        )
                    }
                }
            }

            is PsiMethodCallExpression -> {
                val qualifier = psi.methodExpression.qualifier
                if (qualifier != null) {
                    val processedQualifier = processPsiElement(qualifier, context)
                }

                val methodContext = processMethod(context, psi)
                return methodContext.variables.filter { it.key.isMethod() }.firstOrNull()?.value ?: VoidExpression()
            }

            is PsiNewExpression -> {
                val methodContext = processMethod(
                    context,
                    psi,
                )

                // Wacky idea for next time:
                // What if all expressions held contexts, and that was the only context we had/needed
                // like it wasn't a separate thing at all.

                return NewClassExpr(psi, methodContext)
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
            }

            else -> {
                println("WARN: Unsupported PsiElement type: ${psi::class} (${psi.text})")
            }
        }

        return VoidExpression()
    }

    /**
     * Returns the context of the method call.
     */
    private fun processMethod(
        context: Context,
        callExpr: PsiCallExpression,
        methodContext: Context = Context.new(),
    ): Context {
        val method = callExpr.resolveMethod() ?: throw ExpressionIncompleteException(
            "Failed to resolve method for call: ${callExpr.text}"
        )

        val parameters = method.parameterList.parameters
        val arguments = callExpr.argumentList?.expressions ?: throw ExpressionIncompleteException(
            "Failed to resolve arguments for method call: ${callExpr.text}"
        )

        if (parameters.size != arguments.size) {
            throw ExpressionIncompleteException(
                "Method call ${method.name} expects ${parameters.size} parameters, but got ${arguments.size} arguments."
            )
        }

        for ((param, arg) in parameters.zip(arguments)) {
            val argExpr = processPsiElement(arg, context)
            methodContext.putVar(param, argExpr)
        }

        method.body?.let { body ->
            processPsiElement(body, methodContext)
        }

        return methodContext
    }

    private fun processBinaryExpr(
        lhsPrecast: Expr<*>,
        rhsPrecast: Expr<*>,
        tokenType: IElementType
    ): Expr<*> {
        val comparisonOp = ComparisonOp.fromJavaTokenType(tokenType)
        val binaryNumberOp = BinaryNumberOp.fromJavaTokenType(tokenType)
        val booleanOp = BooleanOp.fromJavaTokenType(tokenType)
        if (booleanOp != null) {
            val lhs = lhsPrecast.castToBoolean()
            val rhs = rhsPrecast.castToBoolean()

            return BooleanExpression(lhs, rhs, booleanOp)
        } else {
            return ConversionsAndPromotion.binaryNumericPromotion(
                lhsPrecast.castToNumbers(),
                rhsPrecast.castToNumbers()
            )
                .map { lhs, rhs ->
                    return@map when {
                        comparisonOp != null -> ComparisonExpression(lhs, rhs, comparisonOp)
                        binaryNumberOp != null -> ArithmeticExpression(lhs, rhs, binaryNumberOp)
                        else -> TODO("Unsupported binary operation: $tokenType ($lhsPrecast, $rhsPrecast)")
                    }
                }
        }
    }
}
