package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.exceptions.ExpressionIncompleteException
import com.github.oberdiah.deepcomplexity.solver.LoopSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion
import com.github.oberdiah.deepcomplexity.utilities.Utilities
import com.github.oberdiah.deepcomplexity.utilities.Utilities.getThisType
import com.github.oberdiah.deepcomplexity.utilities.Utilities.orElse
import com.github.oberdiah.deepcomplexity.utilities.Utilities.resolveIfNeeded
import com.github.oberdiah.deepcomplexity.utilities.Utilities.toKey
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import kotlin.test.assertIs
import kotlin.test.assertNotNull


object MethodProcessing {
    /**
     * Only used for debugging; a nice fast way to get context on what we're doing.
     * Should pin it to your watches in the debugger.
     */
    var CURRENT_LINE: PsiElement? = null

    fun getMethodContext(method: PsiMethod): Context {
        val wrapper = newContext(method.getThisType())

        method.body?.let { body ->
            processPsiStatement(body, wrapper)
        }

        return wrapper.c
    }

    fun newContext(thisType: PsiType?): ContextWrapper = ContextWrapper(Context.brandNew(thisType))

    /**
     * This feels a bit silly, but in some ways it's nice to keep all of our mutability
     * contained to this single location.
     */
    data class ContextWrapper(var c: Context) {
        fun addVar(lExpr: LValueExpr<*>, rExpr: Expr<*>) {
            c = c.withVar(lExpr, rExpr)
        }

        fun stack(other: Context) {
            c = c.stack(other)
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
                    CURRENT_LINE = line
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
                                LValueKeyExpr.new(element.toKey()),
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
                val conditionContext = newContext(context.c.thisType)
                val condition = processPsiExpression(
                    psi.condition ?: throw ExpressionIncompleteException(),
                    conditionContext
                ).castToBoolean()
                context.stack(conditionContext.c)

                val trueBranch = psi.thenBranch ?: throw ExpressionIncompleteException()
                val trueBranchContext = newContext(context.c.thisType)
                processPsiStatement(trueBranch, trueBranchContext)

                val falseBranchContext = newContext(context.c.thisType)
                psi.elseBranch?.let { processPsiStatement(it, falseBranchContext) }

                val combined = Context.combine(trueBranchContext.c, falseBranchContext.c) { a, b ->
                    IfExpr.new(a, b, condition)
                }

                context.stack(combined)
            }

            is PsiConditionalExpression -> {
                val conditionContext = newContext(context.c.thisType)
                val condition = processPsiExpression(psi.condition, conditionContext).castToBoolean()
                // The side effects of the condition need to happen immediately. However,
                // we still need a separate context so that we don't resolve anything into
                // the condition from the main context, as the condition is going to be stacked
                // later on anyway.
                context.stack(conditionContext.c)

                val trueBranch = psi.thenExpression ?: throw ExpressionIncompleteException()
                val trueExprContext = newContext(context.c.thisType)
                val trueResult = processPsiExpression(trueBranch, trueExprContext)

                val falseBranch = psi.elseExpression ?: throw ExpressionIncompleteException()
                val falseExprContext = newContext(context.c.thisType)
                val falseResult = processPsiExpression(falseBranch, falseExprContext)

                val combined = Context.combine(trueExprContext.c, falseExprContext.c) { a, b ->
                    IfExpr.new(a, b, condition)
                }

                val evaluatesTo = (
                        if (trueResult.ind == falseResult.ind) {
                            // This is the easy case, we can always handle this.
                            IfExpr.new(trueResult, falseResult, condition)
                        } else {
                            if (trueResult.ind !is NumberSetIndicator<*> || falseResult.ind !is NumberSetIndicator<*>) {
                                TODO(
                                    "As-yet unsupported conditional expression with non-numeric types: " +
                                            "${trueResult.ind}, ${falseResult.ind}"
                                )
                            }

                            // Loosely based on Java Spec 15.25.
                            // Just doing bnp in all cases isn't strictly correct. For example,
                            // Java will re-interpret int constants down to smaller types if they fit in those smaller types
                            // and the other type is that smaller type.
                            ConversionsAndPromotion.binaryNumericPromotion(
                                trueResult.castToNumbers(),
                                falseResult.castToNumbers()
                            ).map { lhs, rhs ->
                                IfExpr.new(lhs, rhs, condition)
                            }
                        }
                        // We need to resolve the variables in [evaluatesTo] before it can see its own
                        // side effects.
                        ).resolveUnknowns(context.c)


                context.stack(combined)

                return evaluatesTo
            }

            is PsiForStatement -> {
                val initialization = psi.initialization
                if (initialization != null) {
                    processPsiStatement(initialization, context)
                }

                // In this case we specifically don't want to inherit the variables, because in this case
                // the context may be repeated many times, and we want to analyse its effects over time.
                val bodyContext = newContext(context.c.thisType)

                val conditionExpr = psi.condition?.let { condition ->
                    processPsiExpression(condition, bodyContext)
                        .tryCastTo(BooleanSetIndicator)
                }.orElse {
                    ConstExpr.TRUE
                }

                psi.body?.let { processPsiStatement(it, bodyContext) }
                psi.update?.let { processPsiStatement(it, bodyContext) }

                LoopSolver.processLoopContext(bodyContext.c, conditionExpr)
                TODO("Loops haven't been implemented yet")
            }

            is PsiReturnStatement -> {
                val returnExpression = psi.returnValue
                if (returnExpression != null) {
                    val returnKey = psi.toKey()

                    val returnExpr = processPsiExpression(returnExpression, context)
                        .castToUsingTypeCast(returnKey.ind, false)

                    context.c = context.c.withAdditionalReturn(returnKey, returnExpr)
                }
            }

            is PsiExpressionStatement -> {
                return processPsiStatement(psi.expression, context)
            }

            is PsiThisExpression -> {
                return context.c.getVar(ThisKey(psi.type!!))
            }

            is PsiMethodCallExpression -> {
                val qualifier = psi.methodExpression.qualifier?.let {
                    // This has to come before the method call is processed,
                    // because this may create an object on the heap that the method call
                    // would use.
                    processPsiExpression(it, context)
                }
                val methodContext = processMethod(context, psi)

                qualifier?.let {
                    val ind = it.ind
                    assertIs<ObjectSetIndicator>(ind)
                    context.addVar(LValueKeyExpr.new(ThisKey(ind.type)), it)
                }

                val methodReturnValue = methodContext.returnValue?.resolveUnknowns(context.c)

                context.stack(methodContext.withoutReturnValue())

                return methodReturnValue
            }

            is PsiLiteralExpression -> {
                val value = psi.value ?: throw ExpressionIncompleteException()
                return ConstExpr.fromAny(value)
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
                return processBinaryExpr(context, lhs, rhsOperand, tokenType)
            }

            is PsiTypeCastExpression -> {
                val expr = processPsiExpression(
                    psi.operand ?: throw ExpressionIncompleteException(),
                    context
                )

                val psiType = psi.castType ?: throw ExpressionIncompleteException()
                val setInd = Utilities.psiTypeToSetIndicator(psiType.type)
                return TypeCastExpr(expr, setInd, false)
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

                    var currentExpr = processBinaryExpr(context, originalLhs, operands[1], tokenType)

                    for (i in 2 until operands.size) {
                        val newExpr = processBinaryExpr(context, currentExpr, operands[i], tokenType)
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
                            val expr = ArithmeticExpr(
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
                val objType = psi.type!!
                val newObj = ConstExpr.fromHeapMarker(HeapMarker.new(objType))

                val methodContext = processMethod(context, psi)

                context.addVar(LValueKeyExpr.new(ThisKey(objType)), newObj)
                context.stack(methodContext)

                return newObj
            }

            is PsiWhiteSpace, is PsiComment, is PsiJavaToken -> {
                // Ignore whitespace, comments, etc.
                // PsiJavaTokens are all the surrounding tokens like `;`, `{`, `)`, etc.
            }

            else -> {
                throw IllegalArgumentException(
                    "As-yet unsupported PsiElement type for processing PSI statements: ${psi::class}"
                )
            }
        }
        return null
    }

    private fun processReference(psi: PsiExpression, context: ContextWrapper): LValueExpr<*> {
        assertIs<PsiReferenceExpression>(psi, "Expected PsiReferenceExpression, but got ${psi::class}")

        return when (val resolved = psi.resolveIfNeeded()) {
            is PsiField -> {
                LValueFieldExpr.new(
                    QualifiedKey.Field(resolved),
                    psi.qualifier?.let {
                        processPsiExpression(it, context)
                    }.orElse {
                        val thisType = context.c.thisType
                        assertNotNull(
                            thisType,
                            "No qualifier on field ${resolved.name}, but also no `this` type in context?"
                        )
                        context.c.getVar(ThisKey(thisType))
                    }
                )
            }

            is PsiLocalVariable -> LValueKeyExpr.new(resolved.toKey())
            is PsiReturnStatement -> LValueKeyExpr.new(resolved.toKey())
            is PsiParameter -> LValueKeyExpr.new(resolved.toKey())

            else -> {
                throw IllegalArgumentException(
                    "As-yet unsupported PsiReferenceExpression type for processing references: ${resolved::class}"
                )
            }
        }
    }


    /**
     * Returns the new updated original context, and then the context of the method call.
     */
    private fun processMethod(
        context: ContextWrapper,
        callExpr: PsiCallExpression
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
            context.addVar(
                LValueKeyExpr.new(ParameterKey(param, true)),
                processPsiExpression(arg, context)
            )
        }

        val methodContext = newContext(method.getThisType())
        method.body?.let { body ->
            processPsiStatement(body, methodContext)
        }
        return methodContext.c
    }

    /**
     * The RHS is a PsiExpression because it may or may not end up being evaluated due to
     * the potential for short-circuiting.
     */
    private fun processBinaryExpr(
        context: ContextWrapper,
        lhsPrecast: Expr<*>,
        rhsPsi: PsiExpression,
        tokenType: IElementType
    ): Expr<*> {
        val comparisonOp = ComparisonOp.fromJavaTokenType(tokenType)
        val binaryNumberOp = BinaryNumberOp.fromJavaTokenType(tokenType)
        val booleanOp = BooleanOp.fromJavaTokenType(tokenType)
        if (booleanOp != null) {
            // We need to worry about short-circuiting here.

            val lhs = lhsPrecast.castToBoolean()

            val rhsContext = newContext(context.c.thisType)
            val rhs = processPsiExpression(rhsPsi, rhsContext)
                .resolveUnknowns(context.c)
                .castToBoolean()

            /**
             * Effectively operate as an if statement here, where the condition is the lhs.
             * Then we either place the rhs in the 'then' branch or the 'else' branch
             * depending on the boolean operation.
             *
             * `var foo = doFoo() && doBar()`
             * becomes
             * `var foo = doFoo() ? doBar() : false`
             * and
             * `var foo = doFoo() || doBar()`
             * becomes
             * `var foo = doFoo() ? true : doBar()`
             */
            val combined = when (booleanOp) {
                BooleanOp.AND -> {
                    Context.combine(rhsContext.c, Context.brandNew(context.c.thisType)) { a, b ->
                        IfExpr.new(a, b, lhs)
                    }
                }

                BooleanOp.OR -> {
                    Context.combine(Context.brandNew(context.c.thisType), rhsContext.c) { a, b ->
                        IfExpr.new(a, b, lhs)
                    }
                }
            }

            context.stack(combined)

            return BooleanExpr(lhs, rhs, booleanOp)
        } else {
            val rhsPrecast = processPsiExpression(rhsPsi, context)

            return ConversionsAndPromotion.binaryNumericPromotion(
                lhsPrecast.castToNumbers(),
                rhsPrecast.castToNumbers()
            )
                .map { lhs, rhs ->
                    return@map when {
                        comparisonOp != null -> ComparisonExpr(lhs, rhs, comparisonOp)
                        binaryNumberOp != null -> ArithmeticExpr(lhs, rhs, binaryNumberOp)
                        else -> TODO("Unsupported binary operation: $tokenType ($lhsPrecast, $rhsPrecast)")
                    }
                }
        }
    }
}
