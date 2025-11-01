package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.context.Context.KeyBackreference
import com.oberdiah.deepcomplexity.context.ContextVarsAssistant.withVar
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator

object ContextVarsAssistant {
    fun getVar(vars: Vars, key: UnknownKey, makeBackreference: (UnknownKey) -> KeyBackreference): Expr<*> {
        // If we have it, return it.
        vars[key]?.let { return it.getDynExpr() }

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedFieldKey) {
            val placeholderQualifierKey =
                makeBackreference(PlaceholderKey(key.qualifier.ind as ObjectSetIndicator))

            val replacementQualified = VariableExpr.new(makeBackreference(key))
            val replacementRaw = key.qualifier.toLeafExpr()
            val placeholderVersionOfTheKey = QualifiedFieldKey(placeholderQualifierKey, key.field)
            val p = makeBackreference(placeholderVersionOfTheKey)

            vars[placeholderVersionOfTheKey]?.let {
                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.key) {
                        p -> replacementQualified
                        placeholderQualifierKey -> replacementRaw
                        else -> null
                    }
                }

                return replacedExpr.getDynExpr()
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(makeBackreference(key))
    }


    /**
     * This first private [withVar] deals with complex qualifier expressions.
     *
     * The core of this does look a bit scary, so I'll try to walk you through it:
     * Essentially, a qualifier may not just be a simple VariableExpression with a HeapKey.
     * In the simplest case it is, and this all becomes a lot easier, but in the general case
     * it may be any complicated expression.
     * Let's go with the following example:
     * ```
     * a = new C(2);
     * b = new C(3);
     * ((x > 0) ? a : b).x = 5
     * ```
     * Now, the only objects we should be touching with our operation are `a` and `b`, so we gather
     * them first into a set. That part's simple enough.
     * Then, for the variables we want to modify, we take our qualifier as specified above, and replace
     * `a` and `b` with either:
     *      a) The value already at that object, effectively turning `b` into `b.x`
     *      b) The value that we're setting this field to
     *  depending on whether the object we're modifying is the object being replaced in the expression.
     *  The result of this is that for something like `((x > 0) ? a : b).x = 5`, the variables end up
     *  like so:
     *      `a.x = { (x > 0) ? 5 : 3 }`
     *      `b.x = { (x > 0) ? 2 : 5 }`
     *  which is exactly as desired.
     */
    fun withVar(
        vars: Vars,
        lExpr: LValueExpr<*>,
        rExpr: RootExpression<*>,
        makeBackreference: (UnknownKey) -> KeyBackreference
    ): Vars {
        if (lExpr is LValueKeyExpr) {
            return withVar(vars, lExpr.key, rExpr, makeBackreference)
        } else if (lExpr !is LValueFieldExpr) {
            throw IllegalArgumentException("This cannot happen")
        }

        val qualifierExpr = lExpr.qualifier
        val field = lExpr.field

        val qualifiersMentionedInQualifierExpr: Set<Qualifier> =
            qualifierExpr.iterateTree()
                .filterIsInstance<LeafExpr<*>>()
                .map { it.underlying }
                .filterIsInstance<Qualifier>()
                .toSet()

        var vars = vars

        // For every distinct qualifier we mention...
        for (qualifier in qualifiersMentionedInQualifierExpr) {
            val thisVarKey = QualifiedFieldKey(qualifier, field)
            // grab whatever it's currently set to,
            val existingExpr = getVar(vars, thisVarKey, makeBackreference)

            val newValue = rExpr.mapDynamic {
                // and replace it with the qualifier expression itself, but with each leaf
                // replaced with either what we used to be, or [rExpr].
                qualifierExpr.replaceTypeInLeaves<LeafExpr<*>>(field.ind) { expr ->
                    if (expr.underlying == qualifier) {
                        it
                    } else {
                        existingExpr
                    }
                }.castOrThrow(it.ind)
            }

            // In the simple cases this will just perform a basic assignment, but
            // in reality under the hood it may do other stuff due to aliasing.
            vars = withVar(vars, thisVarKey, newValue, makeBackreference)
        }

        return vars
    }

    /**
     * This second private [withVar] handles any potential aliasing.
     * Using this alone should be perfectly correct.
     */
    private fun withVar(
        vars: Vars,
        key: UnknownKey,
        rExpr: RootExpression<*>,
        makeBackreference: (UnknownKey) -> KeyBackreference
    ): Vars {
        val newVariables = vars.toMutableMap()

        fun addExprToNewVariables(key: UnknownKey, expr: RootExpression<*>) {
            // We've checked for aliasing, we've done all of our pre-processing; it's finally
            // time to assign this expression to our variables.

            // First, check if we already have a value assigned to this key. If not, we pretend we did.
            val existingRootExpr = newVariables[key]
                ?: RootExpression.new(VariableExpr.new(makeBackreference(key)))

            // Stack the new expression on top. Stacking expressions combines their static expressions
            // and takes the top dynamic expression.
            newVariables += (key to existingRootExpr.stackedUnder(expr))
        }

        addExprToNewVariables(key, rExpr)

        if (key !is QualifiedFieldKey) {
            // No need to do anything further if there's no risk of aliasing.
            return newVariables
        }

        val qualifier = key.qualifier
        val fieldKey = key.field
        val qualifierInd = key.qualifierInd

        // Collect a list of all objects we know of that could alias with the object we're trying to set.
        val potentialAliasers: Set<QualifiedFieldKey> = vars.keys
            .filterIsInstance<QualifiedFieldKey>()
            .filter {
                !it.isPlaceholder()
                        && qualifier != it.qualifier
                        && fieldKey == it.field
                        && qualifier.ind == it.qualifier.ind
            }
            .toSet() + QualifiedFieldKey(makeBackreference(PlaceholderKey(qualifierInd)), fieldKey)

        for (aliasingKey in potentialAliasers) {
            val condition = ComparisonExpr.new(
                aliasingKey.qualifier.toLeafExpr(),
                qualifier.toLeafExpr(),
                ComparisonOp.EQUAL
            )

            // Each of the aliasers' values needs to be updated to take into account this new assignment,
            // as they may have been affected if it turns out they were equal.
            val newRExpr = rExpr.mapDynamic {
                // If the objects turn out to be the same, the aliasing object is set to whatever value we're
                // setting. Otherwise, we leave it alone.
                IfExpr.new(it, getVar(vars, aliasingKey, makeBackreference), condition)
            }

            addExprToNewVariables(aliasingKey, newRExpr)
        }

        return newVariables
    }
}