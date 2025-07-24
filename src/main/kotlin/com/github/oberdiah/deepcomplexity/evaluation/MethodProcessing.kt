package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.solver.LoopSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.orElse
import com.github.oberdiah.deepcomplexity.utilities.Utilities.resolveIfNeeded
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toKey
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

    private enum class Mode {
        /**
         * Standard operating mode, where we evaluate expressions and return their values.
         */
        RVALUE,

        /**
         * We don't fully evaluate expressions, rather we generate LValue expressions instead
         * that can be used to assign values. You can always convert from this to an RValue
         * later on by calling `.resolveLValues(context)`.
         */
        LVALUE
    }

    private fun processPsiElement(
        psi: PsiElement,
        contextIn: Context,
        mode: Mode = Mode.RVALUE
    ): Context {
        var context = contextIn.nowResolvesTo(VoidExpression())
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
                            context = context.withVar(element.toKey(), context.resolvesTo)
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
                val condition = context.resolvesTo.castToBoolean()

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
                        .resolvesTo.tryCastTo(BooleanSetIndicator)
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
                    val returnExpr = context.resolvesTo

                    if (context.variables.keys.none { it.isReturnKey() }) {
                        // If there's no 'method' key yet, create one.
                        context = context.withVar(psi.toKey(), returnExpr)
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


            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                val exprResult = when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        context = processPsiElement(operand, context)
                        val operand = ConversionsAndPromotion.unaryNumericPromotion(
                            context.resolvesTo.castToNumbers()
                        )

                        unaryOp.applyToExpr(operand)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        context = processPsiElement(operand, context, Mode.LVALUE)
                        val operandExpr = context.resolvesTo.castToNumbers()

                        context = context.withVar(
                            context,
                            operandExpr,
                            unaryOp.applyToExpr(operandExpr.resolveLValues(context))
                        )

                        // Build the expression after the assignment for a prefix increment/decrement
                        context = processPsiElement(operand, context)
                        context.resolvesTo.castToNumbers()
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
                val builtExpr = context.resolvesTo

                // Assign the value to the variable
                context = processPsiElement(psi.operand, context, Mode.LVALUE)
                val operandExpr = context.resolvesTo.castToNumbers()
                context = context.withVar(
                    context,
                    operandExpr,
                    unaryOp.applyToExpr(operandExpr.resolveLValues(context))
                )

                context = context.nowResolvesTo(builtExpr.castToNumbers())
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                context = processPsiElement(lhsOperand, context)
                val lhs = context.resolvesTo
                context = processPsiElement(rhsOperand, context)
                val rhs = context.resolvesTo
                context = context.nowResolvesTo(processBinaryExpr(lhs, rhs, tokenType))
            }

            is PsiTypeCastExpression -> {
                context = processPsiElement(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )
                val expr = context.resolvesTo

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
                    val originalLhs = context.resolvesTo
                    context = processPsiElement(operands[1], context)
                    val originalRhs = context.resolvesTo

                    var currentExpr = processBinaryExpr(originalLhs, originalRhs, tokenType)

                    for (i in 2 until operands.size) {
                        context = processPsiElement(operands[i], context)
                        val nextRhs = context.resolvesTo
                        val newExpr = processBinaryExpr(currentExpr, nextRhs, tokenType)
                        currentExpr = newExpr
                    }

                    context = context.nowResolvesTo(currentExpr)
                }
            }

            is PsiReferenceExpression -> {
                val key = psi.resolveIfNeeded().toKey()

                val qualifier = psi.qualifier?.let {
                    // If there's a qualifier, we need to resolve it first.
                    context = processPsiElement(it, context)
                    context.resolvesTo
                }.orElse {
                    // If there's no qualifier, we want to use the 'this' object if available.
                    if (key is Context.Key.FieldKey) {
                        context.thisObj ?: throw ExpressionIncompleteException(
                            "Field reference without qualifier, but no 'this' object available."
                        )
                    } else {
                        null
                    }
                }

                val lValue = LValueExpr(
                    key,
                    qualifier,
                    key.ind
                )

                context = context.nowResolvesTo(
                    when (mode) {
                        Mode.LVALUE -> lValue
                        Mode.RVALUE -> lValue.resolve(context)
                    }
                )
            }

            is PsiAssignmentExpression -> {
                val lExpression = psi.lExpression
                val rExpression = psi.rExpression ?: throw ExpressionIncompleteException()
                val opSign = psi.operationSign

                context = processPsiElement(lExpression, context, Mode.LVALUE)
                val lhs = context.resolvesTo

                val exprResult = when (opSign.tokenType) {
                    JavaTokenType.EQ -> {
                        context = processPsiElement(rExpression, context)
                        val rhs = context.resolvesTo

                        context = context.withVar(context, lhs, rhs)
                        rhs
                    }

                    JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                        context = processPsiElement(rExpression, context)
                        val rhs = context.resolvesTo.castToNumbers()

                        ConversionsAndPromotion.castNumbersAToB(rhs, lhs.resolveLValues(context).castToNumbers(), false)
                            .map { innerRhs, innerLhs ->
                                val expr = ArithmeticExpression(
                                    innerLhs,
                                    innerRhs,
                                    BinaryNumberOp.fromJavaTokenType(opSign.tokenType)
                                        ?: throw IllegalArgumentException(
                                            "As-yet unsupported assignment operation: $opSign"
                                        )
                                )
                                context = context.withVar(context, lhs, expr)
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
                val qualifier = psi.methodExpression.qualifier?.let {
                    // This has to come before the method call is processed,
                    // because this may create an object on the heap that the method call
                    // would use.
                    context = processPsiElement(it, context)
                    context.resolvesTo
                }

                val (newContext, methodContext) = processMethod(context, psi, qualifier)
                context = newContext

                context =
                    context.nowResolvesTo(
                        methodContext.variables.filter { it.key.isReturnKey() }.firstOrNull()?.value ?: VoidExpression()
                    )
            }

            is PsiNewExpression -> {
                val heapKey = Context.Key.HeapKey.new()

                context = context.withHeap(heapKey, Context.new())

                val classExpr = ClassExpression(psi, heapKey)
                val (newContext, _) = processMethod(context, psi, classExpr)
                context = newContext
                context = context.nowResolvesTo(classExpr)
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
                // PsiJavaTokens are all the surrounding tokens like `;`, `{`, `)`, etc.
            }

            is PsiThisExpression -> {
                // This is a reference to the current object, so we need to resolve it
                // to the current context.
                context = context.nowResolvesTo(context.thisObj!!)
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
        qualifier: Expr<*>? = null
    ): Pair<Context, Context> {
        var context = contextIn
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

        var methodContext = Context.new()
        for ((param, arg) in parameters.zip(arguments)) {
            context = processPsiElement(arg, context)
            methodContext = methodContext.withVar(param.toKey(), context.resolvesTo)
        }
        // The heap is global, it gets passed into methods.
        methodContext = methodContext
            .withHeap(context.heap)
            .withThis(qualifier)

        method.body?.let { body ->
            methodContext = processPsiElement(body, methodContext)
        }
        context = context.withHeap(methodContext.heap)

        // The heap is global, so it comes out of methods.
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
