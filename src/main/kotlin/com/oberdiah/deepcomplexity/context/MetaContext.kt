package com.oberdiah.deepcomplexity.context

import com.intellij.psi.PsiType
import com.oberdiah.deepcomplexity.context.Context.ContextId
import com.oberdiah.deepcomplexity.context.Context.KeyBackreference
import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToContext
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectIndicator
import kotlin.test.assertEquals

class MetaContext(
    val i: InnerCtx,
    /**
     * Unfortunately necessary, and I don't think there's any way around it (though I guess we could store
     * it in the key of the `this` object? What would we do when we don't know that expression yet, though?)
     *
     * The reason this is needed is when we're doing aliasing resolution inside a method with no
     * additional context, we need to know if `this` has the same type as any of the parameters, in case
     * they alias.
     *
     * Imagine a case where we're evaluating an expression like `int t = this.q`. `this`'s type needs to be known
     * to at least some degree to perform alias protection, and the only place to store that is in the context.
     */
    val thisType: PsiType?,
    val idx: ContextId
) {
    companion object {
        fun brandNew(thisType: PsiType?): MetaContext {
            val contextIdx = ContextId.new()
            return MetaContext(InnerCtx.new(), thisType, contextIdx)
        }

        fun combine(lhs: MetaContext, rhs: MetaContext, how: (a: Expr<*>, b: Expr<*>) -> Expr<*>): MetaContext {
            assertEquals(lhs.thisType, rhs.thisType, "Differing 'this' types in contexts.")
            return MetaContext(
                InnerCtx(
                    how(lhs.i.staticExpr, rhs.i.staticExpr).castToContext(),
                    (lhs.i.dynamicVars.keys + rhs.i.dynamicVars.keys)
                        .associateWith { key ->
                            val doNothingExpr = VariableExpr.new(KeyBackreference(key, lhs.idx + rhs.idx))

                            val rhsExpr = rhs.i.dynamicVars[key] ?: doNothingExpr
                            val lhsExpr = lhs.i.dynamicVars[key] ?: doNothingExpr

                            val finalDynExpr = how(lhsExpr, rhsExpr)

                            finalDynExpr.castOrThrow(doNothingExpr.ind)
                        }
                ),
                lhs.thisType,
                lhs.idx + rhs.idx
            )
        }
    }

    override fun toString(): String = i.toString()

    val returnValue: Expr<*>? = i.dynamicVars.filterKeys { it is ReturnKey }.values.firstOrNull()

    private fun mapVars(operation: (Vars) -> Vars): MetaContext =
        MetaContext(i.mapAllVars(operation), thisType, idx)

    fun withoutReturnValue() = mapVars { vars -> vars.filterKeys { it !is ReturnKey } }

    fun stripKeys(lifetime: UnknownKey.Lifetime) =
        mapVars { vars -> vars.filterKeys { !it.shouldBeStripped(lifetime) } }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> {
        return expr.replaceTypeInTree<VariableExpr<*>> { varExpr ->
            varExpr.key.safelyResolveUsing(this)
        }.optimise()
    }

    fun stack(other: MetaContext): MetaContext {
        val other = other
            .stripKeys(UnknownKey.Lifetime.BLOCK)
            .mapVars { it.mapValues { (_, expr) -> resolveKnownVariables(expr) } }
            .mapVars { other ->
                var newVars = i.dynamicVars
                for ((key, expr) in other) {
                    // ...and any keys that might also need resolved...
                    val lValue = if (key is QualifiedFieldKey) {
                        LValueFieldExpr.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
                    } else {
                        LValueKeyExpr.new(key)
                    }

                    // ...and then assign to us.
                    newVars = addToVars(newVars, lValue, expr)
                }
                // Simple!
                newVars
            }

        val afterStack = MetaContext(
            InnerCtx(
                i.staticExpr.replaceTypeInTree<VarsExpr> {
                    if (it.vars != null) {
                        it
                    } else {
                        resolveKnownVariables(other.i.staticExpr)
                    }
                },
                other.i.dynamicVars
            ),
            thisType,
            idx + other.idx
        )

        return afterStack
    }

    fun forcedDynamic(): MetaContext = MetaContext(i.forcedDynamic(::getVarFromVars), thisType, idx)
    fun haveHitReturn(): MetaContext = MetaContext(i.forcedStatic(), thisType, idx)

    // ######################
    // ###  Get Var & Co  ###
    // ######################

    fun getVar(key: UnknownKey): Expr<*> = getVarFromVars(i.dynamicVars, key)
    private fun getVarFromVars(vars: Vars, key: UnknownKey): Expr<*> {
        // If we have it, return it.
        vars[key]?.let { return it }

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedFieldKey) {
            val placeholderQualifierKey =
                KeyBackreference(PlaceholderKey(key.qualifier.ind as ObjectIndicator), idx)

            val replacementQualified = VariableExpr.new(KeyBackreference(key, idx))
            val replacementRaw = key.qualifier.toLeafExpr()
            val placeholderVersionOfTheKey = QualifiedFieldKey(placeholderQualifierKey, key.field)
            val p = KeyBackreference(placeholderVersionOfTheKey, idx)

            vars[placeholderVersionOfTheKey]?.let {
                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.key) {
                        p -> replacementQualified
                        placeholderQualifierKey -> replacementRaw
                        else -> null
                    }
                }

                return replacedExpr
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(KeyBackreference(key, idx))
    }

    // #######################
    // ###  With Var & Co  ###
    // #######################

    fun withVar(lExpr: LValueExpr<*>, rExpr: Expr<*>): MetaContext {
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)
        assert(rExpr.iterateTree<LValueExpr<*>>().none()) {
            "Cannot assign an LValueExpr to a variable: $lExpr = $rExpr. Try using `.resolve(context)` on it first."
        }
        return MetaContext(i.mapDynamicVars { vars -> addToVars(vars, lExpr, rExpr) }, thisType, idx)
    }

    /**
     * This first private [addToVars] deals with complex qualifier expressions.
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
    private fun addToVars(vars: Vars, lExpr: LValueExpr<*>, rExpr: Expr<*>): Vars {
        if (lExpr is LValueKeyExpr) {
            return addToVars(vars, lExpr.key, rExpr)
        } else if (lExpr !is LValueFieldExpr) {
            throw IllegalArgumentException("This cannot happen")
        }

        val qualifierExpr = lExpr.qualifier
        val field = lExpr.field

        val qualifiersMentionedInQualifierExpr: Set<Qualifier> =
            qualifierExpr.iterateTree<LeafExpr<*>>()
                .map { it.underlying }
                .filterIsInstance<Qualifier>()
                .toSet()

        var vars = vars

        // For every distinct qualifier we mention...
        for (qualifier in qualifiersMentionedInQualifierExpr) {
            val thisVarKey = QualifiedFieldKey(qualifier, field)
            // grab whatever it's currently set to,
            val existingExpr = getVarFromVars(vars, thisVarKey)

            // and replace it with the qualifier expression itself, but with each leaf
            // replaced with either what we used to be, or [rExpr].
            val newValue = qualifierExpr.replaceTypeInLeaves<LeafExpr<*>>(field.ind) { expr ->
                if (expr.underlying == qualifier) {
                    rExpr
                } else {
                    existingExpr
                }
            }.castOrThrow(rExpr.ind)

            // In the simple cases this will just perform a basic assignment, but
            // in reality under the hood it may do other stuff due to aliasing.
            vars = addToVars(vars, thisVarKey, newValue)
        }

        return vars
    }

    /**
     * This second private [addToVars] handles any potential aliasing.
     * Using this alone should be perfectly correct.
     */
    private fun addToVars(vars: Vars, key: UnknownKey, rExpr: Expr<*>): Vars {
        var newVariables = vars + (key to rExpr)

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
            .toSet() + QualifiedFieldKey(KeyBackreference(PlaceholderKey(qualifierInd), idx), fieldKey)

        for (aliasingKey in potentialAliasers) {
            val condition = ComparisonExpr.new(
                aliasingKey.qualifier.toLeafExpr(),
                qualifier.toLeafExpr(),
                ComparisonOp.EQUAL
            )

            // Each of the aliasers' values needs to be updated to take into account this new assignment,
            // as they may have been affected if it turns out they were equal.
            // If the objects turn out to be the same, the aliasing object is set to whatever value we're
            // setting. Otherwise, we leave it alone.
            val newRExpr = IfExpr.new(rExpr, getVarFromVars(newVariables, aliasingKey), condition)

            newVariables += aliasingKey to newRExpr
        }

        return newVariables
    }
}