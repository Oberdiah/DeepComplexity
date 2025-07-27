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

object MethodProcessing {
    fun printMethod(method: PsiMethod, evaluate: Boolean) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.
        // What makes method processing nice is we don't need to think about
        // anything outside this method. Things coming in from outside are just unknowns
        // we parameterize over.

        method.body?.let { body ->
            println(processPsiStatement(body, ContextWrapper(Context.brandNew())).toString())
        }
    }

    fun getMethodContext(method: PsiMethod): Context {
        val wrapper = ContextWrapper(Context.brandNew())

        method.body?.let { body ->
            processPsiStatement(body, wrapper)
        }

        return wrapper.c
    }

    /**
     * This feels a bit silly, but in some ways it's nice to keep all of our mutability
     * contained to this single location.
     *
     * If you want to move this mutability into the context itself, first consider
     * the fact that the context contains a heap, which itself is a map of keys to contexts.
     * What happens when those contexts get modified? Scary stuff.
     */
    data class ContextWrapper(var c: Context) {
        fun clone(): ContextWrapper = ContextWrapper(c.clone())

        fun addVar(key: Context.Key, value: Expr<*>) {
            c = c.withVar(key, value)
        }

        fun addVar(lExpr: LValueExpr<*>, rExpr: Expr<*>) {
            c = c.withVar(lExpr, rExpr)
        }

        fun resolveVar(key: Context.Key, expr: Expr<*>) {
            c = c.withResolvedVar(key, expr)
        }

        fun setHeap(heap: Heap) {
            c = c.withHeap(heap)
        }

        fun setThis(thisObj: Expr<*>?) {
            c = c.withThis(thisObj)
        }
    }

    private fun processPsiExpression(
        psi: PsiElement,
        context: ContextWrapper
    ): Expr<*> = processPsiStatement(psi, context) ?: throw RuntimeException(
        "Expected to parse an expression, but a statement was parsed instead: ${psi.text} (${psi::class})\n" +
                "Well, either that or you've forgotten to return something and it's defaulting to null."
    )

    private fun processPsiStatement(
        psi: PsiElement,
        context: ContextWrapper
    ): Expr<*>? {
        when (psi) {
            is PsiBlockStatement -> {
                processPsiStatement(psi.codeBlock, context)
            }

            is PsiCodeBlock -> {
                for (line in psi.children) {
                    processPsiStatement(line, context)
                }
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                psi.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            context.addVar(
                                element.toKey(),
                                processPsiExpression(rExpression, context)
                            )
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
                val condition = processPsiExpression(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                ).castToBoolean()

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = context.clone()
                processPsiStatement(trueBranch, trueBranchContext)

                val falseBranchContext = context.clone()
                psi.elseBranch?.let { processPsiStatement(it, falseBranchContext) }

                context.c = Context.combine(trueBranchContext.c, falseBranchContext.c) { a, b ->
                    IfExpression.new(a, b, condition)
                }
            }

            is PsiConditionalExpression -> {
                val condition = processPsiExpression(psi.condition, context).castToBoolean()

                val trueBranch = psi.thenExpression ?: throw ExpressionIncompleteException()
                val trueExprContext = context.clone()
                val trueResult = processPsiExpression(trueBranch, trueExprContext)

                val falseBranch = psi.elseExpression ?: throw ExpressionIncompleteException()
                val falseExprContext = context.clone()
                val falseResult = processPsiExpression(falseBranch, falseExprContext)

                context.c = Context.combine(trueExprContext.c, falseExprContext.c) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                return IfExpression.new(trueResult, falseResult, condition)
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    processPsiStatement(initialization, context)
                }

                val bodyContext = ContextWrapper(Context.new(context.c.heap, context.c.thisObj))

                psi.body?.let { processPsiStatement(it, bodyContext) }
                psi.update?.let { processPsiStatement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    // This isn't correct anymore now that context is immutable.
                    processPsiExpression(condition, bodyContext).tryCastTo(BooleanSetIndicator)
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext.c, conditionExpr)
            }

            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnExpr = processPsiExpression(returnExpression, context)
                    val returnKey = psi.toKey()

                    if (context.c.returnValue == null) {
                        // If we don't have a return value yet, we create one.
                        context.addVar(returnKey, returnExpr)
                    } else {
                        // If we do, the existing return value will have unresolved return variables
                        // in it (by necessity, as this return we're processing here wasn't part of it), so
                        // we resolve those. I don't think we need to resolve the return across the entire context,
                        // just the return value, but this method is convenient.
                        context.resolveVar(returnKey, returnExpr)
                    }
                }
            }

            is PsiExpressionStatement -> {
                return processPsiStatement(psi.expression, context)
            }

            is PsiThisExpression -> {
                // The `this` in the context had better exist if we're using `this`.
                return context.c.thisObj!!
            }

            is PsiMethodCallExpression -> {
                // Can discard the return value if we're calling it as a statement.
                val qualifier = psi.methodExpression.qualifier?.let {
                    // This has to come before the method call is processed,
                    // because this may create an object on the heap that the method call
                    // would use.
                    processPsiExpression(it, context)
                }
                val methodContext = processMethod(context, psi, qualifier)
                return methodContext.returnValue
            }

            is PsiLiteralExpression -> {
                val value = psi.value ?: throw ExpressionIncompleteException()
                return ConstantExpression.fromAny(value)
            }

            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                return when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        val operand = ConversionsAndPromotion.unaryNumericPromotion(
                            processPsiExpression(operand, context).castToNumbers()
                        )

                        unaryOp.applyToExpr(operand)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val operandExpr = processReference(operand, context).castToNumbers()

                        context.addVar(
                            operandExpr,
                            unaryOp.applyToExpr(operandExpr.resolve(context.c))
                        )

                        // Build the expression after the assignment for a prefix increment/decrement
                        processPsiExpression(operand, context).castToNumbers()
                    }
                }
            }

            is PsiPostfixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")


                // Build the expression before the assignment for a postfix increment/decrement
                val builtExpr = processPsiExpression(psi.operand, context)

                // Assign the value to the variable
                val operandExpr = processReference(psi.operand, context).castToNumbers()
                context.addVar(
                    operandExpr,
                    unaryOp.applyToExpr(operandExpr.resolve(context.c))
                )

                return builtExpr.castToNumbers()
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                val lhs = processPsiExpression(lhsOperand, context)
                val rhs = processPsiExpression(rhsOperand, context)
                return processBinaryExpr(lhs, rhs, tokenType)
            }

            is PsiTypeCastExpression -> {
                val expr = processPsiExpression(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val setInd = Utilities.psiTypeToSetIndicator(psiType.type)
                return TypeCastExpression(expr, setInd, false)
            }

            is PsiParenthesizedExpression -> {
                return processPsiExpression(psi.expression ?: throw ExpressionIncompleteException(), context)
            }

            is PsiPolyadicExpression -> {
                val operands = psi.operands
                if (operands.size < 2) {
                    throw ExpressionIncompleteException()
                } else {
                    // Process it as a bunch of binary expressions in a row, left to right.
                    val tokenType = psi.operationTokenType
                    val originalLhs = processPsiExpression(operands[0], context)
                    val originalRhs = processPsiExpression(operands[1], context)

                    var currentExpr = processBinaryExpr(originalLhs, originalRhs, tokenType)

                    for (i in 2 until operands.size) {
                        val nextRhs = processPsiExpression(operands[i], context)
                        val newExpr = processBinaryExpr(currentExpr, nextRhs, tokenType)
                        currentExpr = newExpr
                    }

                    return currentExpr
                }
            }

            is PsiReferenceExpression -> {
                return processReference(psi, context).resolve(context.c)
            }

            is PsiAssignmentExpression -> {
                val lExpression = psi.lExpression
                val rExpression = psi.rExpression ?: throw ExpressionIncompleteException()
                val opSign = psi.operationSign

                val lhsLvalue = processReference(lExpression, context)

                return when (opSign.tokenType) {
                    JavaTokenType.EQ -> {
                        val rhs = processPsiExpression(rExpression, context)
                        context.addVar(lhsLvalue, rhs)
                        rhs
                    }

                    JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                        val rhs = processPsiExpression(rExpression, context).castToNumbers()
                        val lhs = lhsLvalue.resolve(context.c).castToNumbers()

                        ConversionsAndPromotion.castNumbersAToB(
                            rhs,
                            lhs,
                            false
                        ).map { innerRhs, innerLhs ->
                            val expr = ArithmeticExpression(
                                innerLhs,
                                innerRhs,
                                BinaryNumberOp.fromJavaTokenType(opSign.tokenType)
                                    ?: throw IllegalArgumentException(
                                        "As-yet unsupported assignment operation: $opSign"
                                    )
                            )
                            context.addVar(lhsLvalue, expr)
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

            is PsiNewExpression -> {
                val heapKey = Context.Key.HeapKey.new()

                context.setHeap(context.c.heap + (heapKey to emptyMap()))

                val classExpr = ClassExpression(psi, heapKey)

                processMethod(context, psi, classExpr)

                return classExpr
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
                // PsiJavaTokens are all the surrounding tokens like `;`, `{`, `)`, etc.
            }

            else -> {
                throw IllegalArgumentException(
                    "As-yet unsupported PsiElement type: ${psi::class} (${psi.text})"
                )
            }
        }
        return null
    }

    private fun processReference(psi: PsiExpression, context: ContextWrapper): LValueExpr<*> {
        if (psi !is PsiReferenceExpression) {
            throw IllegalArgumentException(
                "Expected PsiReferenceExpression, but got ${psi::class} (${psi.text})"
            )
        }

        val key = psi.resolveIfNeeded().toKey()

        val qualifier = psi.qualifier?.let {
            // If there's a qualifier, we need to resolve it first.
            processPsiExpression(it, context)
        }.orElse {
            // If there's no qualifier, we want to use the 'this' object if available.
            if (key is Context.Key.FieldKey) {
                context.c.thisObj ?: throw ExpressionIncompleteException(
                    "Field reference without qualifier, but no 'this' object available."
                )
            } else {
                null
            }
        }

        return LValueExpr(
            key,
            qualifier,
            key.ind
        )
    }


    /**
     * Returns the new updated original context, and then the context of the method call.
     */
    private fun processMethod(
        context: ContextWrapper,
        callExpr: PsiCallExpression,
        qualifier: Expr<*>? = null
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

        // `brandNew()` is fine here because we're about to manually set the heap and the `this` object.
        val methodContext = ContextWrapper(Context.brandNew())
        for ((param, arg) in parameters.zip(arguments)) {
            methodContext.addVar(
                param.toKey(),
                processPsiExpression(arg, context)
            )
        }
        // The heap is global, it gets passed into methods.
        // It must come after the parameters are set, so any objects
        // created in the parameters are available in the method's heap.
        methodContext.setHeap(context.c.heap)
        methodContext.setThis(qualifier)

        method.body?.let { body ->
            processPsiStatement(body, methodContext)
        }

        // The heap is global, so it comes out of methods.
        context.setHeap(methodContext.c.heap)

        return methodContext.c
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
