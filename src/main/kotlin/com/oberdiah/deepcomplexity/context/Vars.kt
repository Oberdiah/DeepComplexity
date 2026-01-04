package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castTo
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.staticAnalysis.into
import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.Behaviour

class Vars(
    val idx: ContextId,
    map: Map<UnknownKey, Expr<*>>
) {
    private val map = map.mapValues { expr ->
        expr.value.swapInplaceTypeInTree<VariableExpr<*>> {
            VariableExpr.new(it.resolvesTo.withAddedContextId(idx))
        }
    }.mapKeys { it.key.withAddedContextId(idx) }

    val keys = map.keys
    val returnValue = map.filterKeys { it is ReturnKey }.values.firstOrNull()

    /**
     * Retains all entries that satisfy the given predicate.
     */
    fun filterKeys(operation: (UnknownKey) -> Boolean) = Vars(idx, map.filterKeys(operation))
    fun resolveUsing(vars: Vars): Vars =
        mapExpressions(ExprTreeRebuilder.ExprReplacerWithKey { _, e -> vars.resolveKnownVariables(e) })

    fun mapExpressions(operation: ExprTreeRebuilder.ExprReplacerWithKey): Vars =
        Vars(idx, map.mapValues { (key, expr) -> operation.replace(key, expr) })

    private fun <T : Any> resolveResolvesTo(resolvesTo: ResolvesTo<T>): Expr<T> {
        return when (resolvesTo) {
            is VariableExpr.KeyBackreference -> resolvesTo.safelyResolveUsing(this)
            else -> resolvesTo.toLeafExpr()
        }
    }

    fun <T : Any> resolveKnownVariables(expr: Expr<T>): Expr<T> =
        expr.swapInplaceTypeInTreeChained<VariableExpr<*>> { varExpr ->
            resolveResolvesTo(varExpr.resolvesTo)
        }.swapInplaceTypeInTree<VarsExpr> { varsExpr ->
            varsExpr.map { vars -> vars.resolveUsing(this) }
        }.optimise()

    fun stack(other: Vars): Vars =
        other.map.entries.fold(this) { updatingThis, (key, expr) ->
            updatingThis.with(updatingThis.resolveKey(key), expr)
        }

    /**
     * Keys need resolved too, at least when they're qualified and have the possibility of containing variables.
     */
    fun resolveKey(key: UnknownKey): LValue<*> {
        return if (key is QualifiedFieldKey) {
            LValueField.new(key.field, resolveResolvesTo(key.qualifier).castToObject())
        } else {
            LValueKey.new(key)
        }
    }

    companion object {
        fun new(idx: ContextId): Vars = Vars(idx, mapOf())

        /**
         * Merges the two variable maps, combining variables with identical [UnknownKey]s using [how].
         */
        fun combine(lhs: Vars, rhs: Vars, how: (Expr<*>, Expr<*>) -> Expr<*>): Vars {
            val newKeys = (lhs.keys + rhs.keys) - lhs.keys.intersect(rhs.keys)
            return Vars(rhs.idx + lhs.idx, (lhs.keys + rhs.keys).associateWith { key ->
                val lhsResult = lhs.get(key)
                val rhsResult = rhs.get(key)

                /**
                 * This is important, but removing it shouldn't technically result in any incorrect values,
                 * just a lot of noise and impossible-to-reach parts of the evaluation tree with unresolved
                 * variables.
                 * To understand what this is solving, take the example
                 * ```
                 * Foo f = new Foo(5);
                 * if (x > 5) {
                 *     f = new Foo(10);
                 * }
                 * return f.x;
                 * ```
                 * Without this modification, here's how the `if` would end up looking:
                 * ```
                 * f: if (x > 5) { #2 } else { f' }
                 * #2.x: if (x > 5) { 10 } else { #2.x' }
                 * ```
                 *
                 * Now, that #2.x' produced there will never be resolved, as the #2 object was created in the true
                 * branch so the other branch cannot contain the same instance of the object. If `if` branches
                 * were always built on fresh contexts, checking to see if the qualifier was on an object instance
                 * (a constant) would be enough (all other qualifiers would be references). However, we cannot assume
                 * that; it is also acceptable for branches to be implemented by combining two clones of a context.
                 * Given that, we need to additionally check that the qualified constant does not appear
                 * on the other side too (if it does, both branches are both referencing an already-existing object
                 * from earlier in the code path).
                 */
                if (key is QualifiedFieldKey && key.qualifier.isConstant() && key in newKeys) {
                    return@associateWith when (key) {
                        in lhs.keys -> lhsResult
                        in rhs.keys -> rhsResult
                        else -> throw IllegalStateException("How could the key not be in either map?")
                    }
                }

                how(lhsResult, rhsResult)
            })
        }
    }

    override fun toString(): String {
        val nonPlaceholderVariablesString =
            map.filterKeys { !it.isPlaceholder() }.entries.joinToString("\n") { entry ->
                "${entry.key}:\n${entry.value.toString().prependIndent()}"
            }
        val placeholderVariablesString =
            map.filterKeys { it.isPlaceholder() }.entries.joinToString("\n") { entry ->
                "${entry.key}:\n${entry.value.toString().prependIndent()}"
            }

        return "{\n" +
                "${nonPlaceholderVariablesString.prependIndent()}\n" +
                "${placeholderVariablesString.prependIndent()}\n" +
                "}"
    }

    fun <T : Any> get(expr: LValue<T>): Expr<T> {
        return when (expr) {
            is LValueField<*> -> expr.qualifier.replaceTypeInTree<LeafExpr<HeapMarker>>(IfTraversal.BranchesOnly) {
                get(QualifiedFieldKey(it.resolvesTo, expr.field))
            }

            is LValueKey<*> -> get(expr.key)
        }.castOrThrow(expr.ind)
    }

    /**
     * Grab the variable expression assigned to the given key.
     */
    private fun get(key: UnknownKey): Expr<*> {
        // If we have it, return it.
        map[key]?.let { return it }

        getPlaceholderFor(key)?.let { return it }

        // OK, now we really do have no choice
        return VariableExpr.new(key, idx)
    }

    private fun getPlaceholderFor(key: UnknownKey): Expr<*>? {
        if (key is QualifiedFieldKey) {
            // This is straightforward; wherever the placeholder for the entire key exists,
            // we replace it with the key itself.
            val placeholderKey = VariableExpr.KeyBackreference.new(key.toPlaceholderKey(), idx)
            val placeholderKeyReplacement = VariableExpr.new(key, idx)

            // Slightly more complicated; wherever the placeholder for the qualifier alone exists,
            // we also need to replace that with the qualifier itself.
            val placeholderQualifier = ResolvesTo.PlaceholderResolvesTo(key.qualifier.ind.into())
            val placeholderQualifierReplacement = key.qualifier.toLeafExpr()

            map[key.toPlaceholderKey()]?.let {
                val replacedExpr = it.swapInplaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.resolvesTo) {
                        placeholderKey -> placeholderKeyReplacement
                        placeholderQualifier -> placeholderQualifierReplacement
                        else -> null
                    }
                }

                return replacedExpr
            }
        }
        return null
    }

    /**
     * This first private [with] deals with complex qualifier expressions.
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
    fun with(lExpr: LValue<*>, rExpr: Expr<*>): Vars {
        val rExpr = rExpr.castTo(lExpr.ind, Behaviour.WrapWithTypeCastImplicit)

        if (lExpr is LValueKey) {
            return with(lExpr.key, rExpr)
        } else if (lExpr !is LValueField) {
            throw IllegalArgumentException("This cannot happen")
        }

        val qualifierExpr = lExpr.qualifier
        val field = lExpr.field

        val qualifiersMentionedInQualifierExpr: Set<ResolvesTo<HeapMarker>> =
            qualifierExpr.iterateLeaves().map { it.resolvesTo }.toSet()

        var vars = this

        // For every distinct qualifier we mention...
        for (resolvesTo in qualifiersMentionedInQualifierExpr) {
            val thisVarKey = QualifiedFieldKey(resolvesTo, field)
            // grab whatever it's currently set to,
            val existingExpr = vars.get(thisVarKey)

            // and replace it with the qualifier expression itself, but with each leaf
            // replaced with either what we used to be, or [rExpr].
            val newValue = qualifierExpr.replaceTypeInTree<LeafExpr<HeapMarker>>(IfTraversal.BranchesOnly) { expr ->
                if (expr.resolvesTo == resolvesTo) {
                    rExpr
                } else {
                    existingExpr
                }
            }.castOrThrow(rExpr.ind)

            // In the simple cases this will just perform a basic assignment, but
            // in reality under the hood it may do other stuff due to aliasing.
            vars = vars.with(thisVarKey, newValue)
        }

        return vars
    }

    /**
     * This second private [with] handles any potential aliasing.
     * Using this alone should be perfectly correct.
     */
    fun with(key: UnknownKey, rExpr: Expr<*>): Vars {
        var newVars = Vars(idx, map + (key to rExpr))

        if (key !is QualifiedFieldKey) {
            // No need to do anything further if there's no risk of aliasing.
            return newVars
        }

        val qualifier = key.qualifier
        val fieldKey = key.field

        // Collect a list of all objects we know of that could alias with the object we're trying to set.
        val potentialAliasers: Set<QualifiedFieldKey> = map.keys
            .filterIsInstance<QualifiedFieldKey>()
            .filter {
                !it.isPlaceholder()
                        && qualifier != it.qualifier
                        && fieldKey == it.field
                        && qualifier.ind == it.qualifier.ind
            }
            .toSet() + QualifiedFieldKey(ResolvesTo.PlaceholderResolvesTo(key.qualifier.ind.into()), fieldKey)

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
            val newRExpr = IfExpr.new(rExpr, newVars.get(aliasingKey), condition)

            newVars = Vars(idx, newVars.map + (aliasingKey to newRExpr))
        }

        return newVars
    }
}