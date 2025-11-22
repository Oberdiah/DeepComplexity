package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.*
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castOrThrow
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToObject
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.castToUsingTypeCast
import com.oberdiah.deepcomplexity.staticAnalysis.into

class Vars(
    val idx: ContextId,
    map: Map<UnknownKey, Expr<*>>
) {
    private val map = map.mapValues { expr ->
        expr.value.replaceTypeInTree<VariableExpr<*>> {
            VariableExpr.new(it.resolvesTo.withAddedContextId(idx))
        }
    }.mapKeys { it.key.withAddedContextId(idx) }
    val keys = map.keys
    val returnValue = map.filterKeys { it is ReturnKey }.values.firstOrNull()
    fun filterKeys(operation: (UnknownKey) -> Boolean) = Vars(idx, map.filterKeys(operation))
    fun resolveUsing(context: Context): Vars =
        Vars(idx, map.mapValues { (_, expr) -> context.resolveKnownVariables(expr) })

    fun stack(other: Vars): Vars =
        other.map.entries.fold(this) { acc, (key, expr) ->
            acc.with(acc.resolveKey(key), expr)
        }

    companion object {
        fun new(idx: ContextId): Vars = Vars(idx, mapOf())

        /**
         * Merges the two variable maps, combining variables with identical [UnknownKey]s using [how].
         */
        fun combine(lhs: Vars, rhs: Vars, how: (Expr<*>, Expr<*>) -> Expr<*>): Vars =
            Vars(
                rhs.idx + lhs.idx,
                (lhs.keys + rhs.keys).associateWith { key -> how(lhs.get(key), rhs.get(key)) }
            )
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

    fun resolveKey(key: UnknownKey): LValue<*> {
        return if (key is QualifiedFieldKey) {
            LValueField.new(key.field, key.qualifier.safelyResolveUsing(this).castToObject())
        } else {
            LValueKey.new(key)
        }
    }

    fun <T : Any> get(expr: LValue<T>): Expr<T> {
        return when (expr) {
            is LValueField<*> -> expr.qualifier.replaceTypeInLeaves<LeafExpr<HeapMarker>>(expr.field.ind) {
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

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedFieldKey) {
            val placeholderQualifierKey =
                VariableExpr.KeyBackreference.new(PlaceholderKey(key.qualifier.ind.into()), idx, key.qualifier.ind)

            val replacementQualified = VariableExpr.new(key, idx)
            val replacementRaw = key.qualifier.toLeafExpr()
            val placeholderVersionOfTheKey = QualifiedFieldKey(placeholderQualifierKey, key.field)
            val p = VariableExpr.KeyBackreference.new(placeholderVersionOfTheKey, idx)

            map[placeholderVersionOfTheKey]?.let {
                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.resolvesTo) {
                        p -> replacementQualified
                        placeholderQualifierKey -> replacementRaw
                        else -> null
                    }
                }

                return replacedExpr
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(key, idx)
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
        val rExpr = rExpr.castToUsingTypeCast(lExpr.ind, explicit = false)

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
            val newValue = qualifierExpr.replaceTypeInLeaves<LeafExpr<HeapMarker>>(field.ind) { expr ->
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
        val qualifierInd = key.qualifierInd

        // Collect a list of all objects we know of that could alias with the object we're trying to set.
        val potentialAliasers: Set<QualifiedFieldKey> = map.keys
            .filterIsInstance<QualifiedFieldKey>()
            .filter {
                !it.isPlaceholder()
                        && qualifier != it.qualifier
                        && fieldKey == it.field
                        && qualifier.ind == it.qualifier.ind
            }
            .toSet() + QualifiedFieldKey(
            VariableExpr.KeyBackreference.new(PlaceholderKey(qualifierInd), idx, qualifierInd),
            fieldKey
        )

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