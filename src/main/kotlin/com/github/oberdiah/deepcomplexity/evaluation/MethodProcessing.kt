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
            println(processPsiElement(body, ContextWrapper(Context.new())).toString())
        }
    }

    fun getMethodContext(method: PsiMethod): Context {
        val wrapper = ContextWrapper(Context.new())

        method.body?.let { body ->
            processPsiElement(body, wrapper)
        }

        return wrapper.c
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

    /**
     * This feels a bit silly, but in some ways it's nice to keep all of our mutability
     * contained to this single location.
     *
     * If you want to move this mutability into the context itself, first consider
     * the fact that the context contains a heap, which itself is a map of keys to contexts.
     * What happens when those contexts get modified? Scary stuff.
     */
    data class ContextWrapper(var c: Context) {
        fun addVar(key: Context.Key, value: Expr<*>) {
            c = c.withVar(key, value)
        }

        fun addVar(lExpr: Expr<*>, rExpr: Expr<*>) {
            c = c.withVar(lExpr, rExpr)
        }

        fun stack(newContext: Context) {
            c = c.stack(newContext)
        }

        fun resolveVar(psi: PsiElement, value: Expr<*>) {
            c = c.withResolvedVar(psi.toKey(), value)
        }

        fun setHeap(heap: Map<Context.Key.HeapKey, Context>) {
            c = c.withHeap(heap)
        }

        fun setThis(thisObj: Expr<*>?) {
            c = c.withThis(thisObj)
        }
    }

    private fun processPsiElement(
        psi: PsiElement,
        context: ContextWrapper,
        mode: Mode = Mode.RVALUE
    ): Expr<*>? {
        when (psi) {
            is PsiBlockStatement -> {
                return processPsiElement(psi.codeBlock, context)
            }

            is PsiCodeBlock -> {
                for (line in psi.children) {
                    processPsiElement(line, context)
                }
            }

            is PsiExpressionStatement -> {
                return processPsiElement(psi.expression, context)
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                psi.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            context.addVar(
                                element.toKey(),
                                processPsiElement(rExpression, context)!!
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

            is PsiConditionalExpression -> {
                val condition = processPsiElement(psi.condition, context)!!.castToBoolean()

                val trueBranch = psi.thenExpression ?: throw ExpressionIncompleteException()
                val trueExprContext = ContextWrapper(Context.new())
                val trueResult = context.c.resolveKnownVariables(
                    processPsiElement(trueBranch, trueExprContext)!!
                )

                val falseBranch = psi.elseExpression ?: throw ExpressionIncompleteException()
                val falseExprContext = ContextWrapper(Context.new())
                val falseResult = context.c.resolveKnownVariables(
                    processPsiElement(falseBranch, falseExprContext)!!
                )

                val ifContext = Context.combine(trueExprContext.c, falseExprContext.c) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                context.stack(ifContext)

                return IfExpression.new(trueResult, falseResult, condition)
            }

            is PsiIfStatement -> {
                val condition = processPsiElement(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    context
                )!!.castToBoolean()

                // Definitely need to TODO: Heap modifications and thisObj need to be passed in and out of these branches?
                // At the very least in and out of the reached branch.

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = ContextWrapper(Context.new())
                processPsiElement(trueBranch, trueBranchContext)

                val falseBranchContext = ContextWrapper(Context.new())
                psi.elseBranch?.let { processPsiElement(it, falseBranchContext) }

                val ifContext = Context.combine(trueBranchContext.c, falseBranchContext.c) { a, b ->
                    IfExpression.new(a, b, condition)
                }

                context.stack(ifContext)
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    processPsiElement(initialization, context)
                }

                val bodyContext = ContextWrapper(Context.new())

                psi.body?.let { processPsiElement(it, bodyContext) }
                psi.update?.let { processPsiElement(it, bodyContext) }

                val conditionExpr = psi.condition?.let { condition ->
                    // This isn't correct anymore now that context is immutable.
                    processPsiElement(condition, bodyContext)!!.tryCastTo(BooleanSetIndicator)
                }.orElse {
                    ConstantExpression.TRUE
                }

                LoopSolver.processLoopContext(bodyContext.c, conditionExpr)

                context.stack(bodyContext.c)
            }


            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnExpr = processPsiElement(returnExpression, context)!!

                    if (context.c.variables.keys.none { it.isReturnKey() }) {
                        // If there's no 'method' key yet, create one.
                        context.addVar(psi.toKey(), returnExpr)
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

            is PsiPrefixExpression -> {
                val tokenType = psi.operationSign.tokenType
                val unaryOp = UnaryNumberOp.fromJavaTokenType(tokenType)
                    ?: throw IllegalArgumentException("As-yet unsupported unary operation: ${psi.operationSign}")

                val operand = psi.operand ?: throw ExpressionIncompleteException()

                return when (unaryOp) {
                    UnaryNumberOp.NEGATE, UnaryNumberOp.PLUS -> {
                        val operand = ConversionsAndPromotion.unaryNumericPromotion(
                            processPsiElement(operand, context)!!.castToNumbers()
                        )

                        unaryOp.applyToExpr(operand)
                    }

                    UnaryNumberOp.INCREMENT, UnaryNumberOp.DECREMENT -> {
                        val operandExpr = processPsiElement(operand, context, Mode.LVALUE)!!.castToNumbers()

                        context.addVar(
                            operandExpr,
                            unaryOp.applyToExpr(operandExpr.resolveLValues(context.c))
                        )

                        // Build the expression after the assignment for a prefix increment/decrement
                        processPsiElement(operand, context)!!.castToNumbers()
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
                val operandExpr = processPsiElement(psi.operand, context, Mode.LVALUE)!!.castToNumbers()
                context.addVar(
                    operandExpr,
                    unaryOp.applyToExpr(operandExpr.resolveLValues(context.c))
                )

                return builtExpr!!.castToNumbers()
            }

            is PsiBinaryExpression -> {
                val lhsOperand = psi.lOperand
                val rhsOperand = psi.rOperand ?: throw ExpressionIncompleteException()

                val tokenType = psi.operationSign.tokenType

                val lhs = processPsiElement(lhsOperand, context)!!
                val rhs = processPsiElement(rhsOperand, context)!!
                return processBinaryExpr(lhs, rhs, tokenType)
            }

            is PsiTypeCastExpression -> {
                val expr = processPsiElement(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )!!

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val setInd = Utilities.psiTypeToSetIndicator(psiType.type)
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
                    val originalLhs = processPsiElement(operands[0], context)!!
                    val originalRhs = processPsiElement(operands[1], context)!!

                    var currentExpr = processBinaryExpr(originalLhs, originalRhs, tokenType)

                    for (i in 2 until operands.size) {
                        val nextRhs = processPsiElement(operands[i], context)!!
                        val newExpr = processBinaryExpr(currentExpr, nextRhs, tokenType)
                        currentExpr = newExpr
                    }

                    return currentExpr
                }
            }

            is PsiReferenceExpression -> {
                val key = psi.resolveIfNeeded().toKey()

                val qualifier = psi.qualifier?.let {
                    // If there's a qualifier, we need to resolve it first.
                    processPsiElement(it, context)!!
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

                val lValue = LValueExpr(
                    key,
                    qualifier,
                    key.ind
                )

                return when (mode) {
                    Mode.LVALUE -> lValue
                    Mode.RVALUE -> lValue.resolve(context.c)
                }
            }

            is PsiAssignmentExpression -> {
                val lExpression = psi.lExpression
                val rExpression = psi.rExpression ?: throw ExpressionIncompleteException()
                val opSign = psi.operationSign

                val lhsLvalue = processPsiElement(lExpression, context, Mode.LVALUE)!!

                return when (opSign.tokenType) {
                    JavaTokenType.EQ -> {
                        val rhs = processPsiElement(rExpression, context)!!
                        context.addVar(lhsLvalue, rhs)
                        rhs
                    }

                    JavaTokenType.PLUSEQ, JavaTokenType.MINUSEQ, JavaTokenType.ASTERISKEQ, JavaTokenType.DIVEQ -> {
                        val rhs = processPsiElement(rExpression, context)!!.castToNumbers()
                        val lhs = lhsLvalue.resolveLValues(context.c).castToNumbers()

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

            is PsiMethodCallExpression -> {
                val qualifier = psi.methodExpression.qualifier?.let {
                    // This has to come before the method call is processed,
                    // because this may create an object on the heap that the method call
                    // would use.
                    processPsiElement(it, context)
                }

                val methodContext = processMethod(context, psi, qualifier)
                return methodContext.variables.filter { it.key.isReturnKey() }.firstOrNull()?.value
            }

            is PsiNewExpression -> {
                val heapKey = Context.Key.HeapKey.new()

                context.setHeap(context.c.heap + (heapKey to Context.new()))

                val classExpr = ClassExpression(psi, heapKey)

                processMethod(context, psi, classExpr)

                return classExpr
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
                // PsiJavaTokens are all the surrounding tokens like `;`, `{`, `)`, etc.
            }

            is PsiThisExpression -> {
                // The `this` in the context had better exist if we're using `this`.
                return context.c.thisObj!!
            }

            else -> {
                println("WARN: Unsupported PsiElement type: ${psi::class} (${psi.text})")
            }
        }

        return null
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

        val methodContext = ContextWrapper(Context.new())
        for ((param, arg) in parameters.zip(arguments)) {
            methodContext.addVar(
                param.toKey(),
                processPsiElement(arg, context)!!
            )
        }
        // The heap is global, it gets passed into methods.
        methodContext.setHeap(context.c.heap)
        methodContext.setThis(qualifier)

        method.body?.let { body ->
            processPsiElement(body, methodContext)
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
