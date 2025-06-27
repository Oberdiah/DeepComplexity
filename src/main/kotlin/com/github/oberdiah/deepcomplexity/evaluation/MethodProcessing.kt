package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.solver.LoopSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
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

        method.body?.let { body ->
            println(processPsiElement(body, Context.new()).toString())
        }
    }

    fun getMethodContext(method: PsiMethod): Context {
        return method.body?.let { body ->
            processPsiElement(body, Context.new())
        } ?: Context.new()
    }

    private fun processPsiElement(
        psi: PsiElement,
        contextIn: Context
    ): Context {
        var context = contextIn
        when (psi) {
            is PsiBlockStatement -> {
                context = processPsiElement(psi.codeBlock, context)
            }

            is PsiCodeBlock -> {
                for (line in psi.children) {
                    context = processPsiElement(line, context)
                }
            }

            is PsiExpressionStatement -> {
                context = processPsiElement(psi.expression, context)
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                psi.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            context = processPsiElement(rExpression, context)
                            context = context.withVar(element, context.getReturnExpr())
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
                context = processPsiElement(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                )
                val condition = context.getReturnExpr().castToBoolean()

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                var trueBranchContext = Context.new()
                trueBranchContext = processPsiElement(trueBranch, trueBranchContext)
                var falseBranchContext = Context.new()
                falseBranchContext = psi.elseBranch?.let { processPsiElement(it, falseBranchContext) }
                    ?: Context.new()

                val ifContext = Context.combine(trueBranchContext, falseBranchContext) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                context = context.stack(ifContext)
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    context = processPsiElement(initialization, context)
                }

                val bodyContext = Context.new()

                // This isn't correct anymore now that context is immutable.
                psi.body?.let { processPsiElement(it, bodyContext) }
                // This isn't correct anymore now that context is immutable.
                psi.update?.let { processPsiElement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    // This isn't correct anymore now that context is immutable.
                    processPsiElement(condition, bodyContext)
                        .getReturnExpr().tryCastTo(BooleanSetIndicator)
                        ?: TODO("Failed to cast to BooleanSet: ${condition.text}")
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext, conditionExpr)

                context = context.stack(bodyContext)
            }


            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    context = processPsiElement(returnExpression, context)
                    val returnExpr = context.getReturnExpr()

                    if (context.variables.keys.none { it.isMethod() }) {
                        // If there's no 'method' key yet, create one.
                        context = context.withVar(psi, returnExpr)
                    } else {
                        // If there is, resolve the existing one with the new value.
                        context = context.withResolvedVar(psi, returnExpr)
                    }
                }
            }

            is PsiLiteralExpression -> {
                val value = psi.value ?: throw ExpressionIncompleteException()
                context = context.nowResolvesTo(ConstantExpression.fromAny(value))
            }

            is PsiReferenceExpression -> {
                context = context.nowResolvesTo(context.getVar(psi.resolveIfNeeded()))
            }

            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                val exprResult = when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        context = processPsiElement(operand, context)
                        val operand = ConversionsAndPromotion.unaryNumericPromotion(
                            context.getReturnExpr().castToNumbers()
                        )

                        unaryOp.applyToExpr(operand)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val resolvedOperand = operand.resolveIfNeeded()
                        val operandExpr = context.getVar(resolvedOperand).castToNumbers()

                        context = context.withVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                        // Build the expression after the assignment for a prefix increment/decrement
                        context = processPsiElement(operand, context)
                        context.getReturnExpr().castToNumbers()
                    }
                }

                context = context.nowResolvesTo(exprResult)
            }

            is PsiPostfixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")


                // Build the expression before the assignment for a postfix increment/decrement
                context = processPsiElement(psi.operand, context)
                val builtExpr = context.getReturnExpr()

                // Assign the value to the variable
                val resolvedOperand = psi.operand.resolveIfNeeded()
                val operandExpr = context.getVar(resolvedOperand).castToNumbers()
                context = context.withVar(resolvedOperand, unaryOp.applyToExpr(operandExpr))

                context = context.nowResolvesTo(builtExpr.castToNumbers())
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                context = processPsiElement(lhsOperand, context)
                val lhs = context.getReturnExpr()
                context = processPsiElement(rhsOperand, context)
                val rhs = context.getReturnExpr()
                context = context.nowResolvesTo(processBinaryExpr(lhs, rhs, tokenType))
            }

            is PsiTypeCastExpression -> {
                context = processPsiElement(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )
                val expr = context.getReturnExpr()

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val setInd = Utilities.psiTypeToSetIndicator(psiType.type)
                context = context.nowResolvesTo(TypeCastExpression(expr, setInd, false))
            }

            is PsiParenthesizedExpression -> {
                context = processPsiElement(psi.expression ?: throw ExpressionIncompleteException(), context)
            }

            is PsiPolyadicExpression -> {
                val operands = psi.operands
                if (operands.size < 2) {
                    throw ExpressionIncompleteException()
                } else {
                    // Process it as a bunch of binary expressions in a row, left to right.
                    val tokenType = psi.operationTokenType
                    context = processPsiElement(operands[0], context)
                    val originalLhs = context.getReturnExpr()
                    context = processPsiElement(operands[1], context)
                    val originalRhs = context.getReturnExpr()

                    var currentExpr = processBinaryExpr(originalLhs, originalRhs, tokenType)

                    for (i in 2 until operands.size) {
                        context = processPsiElement(operands[i], context)
                        val nextRhs = context.getReturnExpr()
                        val newExpr = processBinaryExpr(currentExpr, nextRhs, tokenType)
                        currentExpr = newExpr
                    }

                    context = context.nowResolvesTo(currentExpr)
                }
            }

            is PsiAssignmentExpression -> {
                val lExpression = psi.lExpression
                val rExpression = psi.rExpression ?: throw ExpressionIncompleteException()
                val opSign = psi.operationSign

                val exprResult = when (opSign.tokenType) {
                    JavaTokenType.EQ -> {
                        context = processPsiElement(rExpression, context)
                        val rhs = context.getReturnExpr()
                        context = context.withVar(lExpression.resolveIfNeeded(), rhs)
                        rhs
                    }

                    JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                        val resolvedLhs = lExpression.resolveIfNeeded()
                        val lhs = context.getVar(resolvedLhs).castToNumbers()
                        context = processPsiElement(rExpression, context)
                        val rhs = context.getReturnExpr().castToNumbers()

                        ConversionsAndPromotion.castNumbersAToB(rhs, lhs, false).map { rhs, lhs ->
                            val expr = ArithmeticExpression(
                                lhs,
                                rhs,
                                BinaryNumberOp.fromJavaTokenType(opSign.tokenType)
                                    ?: throw IllegalArgumentException(
                                        "As-yet unsupported assignment operation: $opSign"
                                    )
                            )
                            context = context.withVar(resolvedLhs, expr)
                            expr
                        }
                    }

                    else -> {
                        TODO(
                            "As-yet unsupported assignment operation: $opSign"
                        )
                    }
                }

                context = context.nowResolvesTo(exprResult)
            }

            is PsiMethodCallExpression -> {
                val qualifier = psi.methodExpression.qualifier
                if (qualifier != null) {
                    context = processPsiElement(qualifier, context)
                    val processedQualifier = context.getReturnExpr()
                }

                val (newContext, methodContext) = processMethod(context, psi)
                context = newContext
                context =
                    context.nowResolvesTo(
                        methodContext.variables.filter { it.key.isMethod() }.firstOrNull()?.value ?: VoidExpression()
                    )
            }

            is PsiNewExpression -> {
                val (newContext, methodContext) = processMethod(context, psi)
                context = newContext

                // Wacky idea for next time:
                // What if all expressions held contexts, and that was the only context we had/needed
                // like it wasn't a separate thing at all.

                context = context.nowResolvesTo(NewClassExpr(psi, methodContext))
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
            }

            else -> {
                println("WARN: Unsupported PsiElement type: ${psi::class} (${psi.text})")
            }
        }

        return context
    }

    /**
     * Returns the new updated original context, and then the context of the method call.
     */
    private fun processMethod(
        contextIn: Context,
        callExpr: PsiCallExpression,
        methodContextIn: Context = Context.new(),
    ): Pair<Context, Context> {
        var context = contextIn
        var methodContext = methodContextIn
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
            context = processPsiElement(arg, context)
            methodContext = methodContext.withVar(param, context.getReturnExpr())
        }

        method.body?.let { body ->
            methodContext = processPsiElement(body, methodContext)
        }

        return context to methodContext
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
