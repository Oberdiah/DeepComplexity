package com.github.oberdiah.deepcomplexity.evaluation

/**
 * In order to implement early returns, each expression in a context needs to be
 * split in half - a section representing the 'rest of the method' and a section
 * that is set in stone and can no longer be changed.
 *
 * In the [staticExpr], [RestOfMethodExpr]s are used to indicate the sections that should
 * be replaced with the [restOfMethodExpr] when stacking.
 *
 * For example, we would represent
 * ```
 * if (y > 10) {
 * 	a.x += 1;
 * 	return;
 * }
 * a.x = a.x + 2;
 * ```
 * as
 * ```
 * staticExpr: (y > 10) ? (a.x` + 1) : REM
 * restOfMethodExpr: a.x` + 2
 * ```
 */
class RootExpression<T : Any>(
    private val staticExpr: Expr<*>,
    private val restOfMethodExpr: Expr<T>
) {
    companion object {
        /**
         * Should be used pretty sparingly. The majority of the time using [withREMExpr] makes more sense.
         */
        fun new(expr: Expr<*>): RootExpression<*> {
            return RootExpression(
                staticExpr = RestOfMethodExpr,
                restOfMethodExpr = expr
            )
        }

        fun combine(
            doNothingExpr: Expr<*>,
            lhs: RootExpression<*>?,
            rhs: RootExpression<*>?,
            how: (a: Expr<*>, b: Expr<*>) -> Expr<*>
        ): RootExpression<*> {
            val lhsStaticExpr = lhs?.staticExpr ?: RestOfMethodExpr
            val rhsStaticExpr = rhs?.staticExpr ?: RestOfMethodExpr

            val finalStaticExpr = how(lhsStaticExpr, rhsStaticExpr)

            val rhsROMExpr = rhs?.restOfMethodExpr ?: doNothingExpr
            val lhsROMExpr = lhs?.restOfMethodExpr ?: doNothingExpr

            val finalROMExpr = how(lhsROMExpr, rhsROMExpr)

            return RootExpression(
                staticExpr = finalStaticExpr,
                restOfMethodExpr = finalROMExpr
            )
        }
    }

    /**
     * Returns the 'rest of method' of this expression; this is the bit you typically want when you getVar().
     */
    fun getREMExpr(): Expr<*> = restOfMethodExpr

    /**
     * The opposite of [getREMExpr]; returns a new RootExpression with the given REM expression.
     */
    fun withREMExpr(expr: Expr<T>): RootExpression<T> = RootExpression(
        staticExpr = staticExpr,
        restOfMethodExpr = expr
    )


    /**
     * You'll typically only call this if you're a method and want to collapse this expression
     * as short-circuiting returns are no longer a concern.
     */
    fun collapseAndGetFullExpr(): Expr<*> {
        return staticExpr.replaceTypeInLeaves<RestOfMethodExpr>(restOfMethodExpr.ind) {
            restOfMethodExpr
        }
    }

    fun withHitReturnMethod(doNothingExpr: Expr<T>): RootExpression<T> {
        return RootExpression(
            staticExpr = restOfMethodExpr,
            restOfMethodExpr = doNothingExpr
        )
    }

    internal inline fun <reified Q> replaceTypeInTree(crossinline replacement: (Q) -> Expr<*>?): RootExpression<T> {
        val newStaticExpr = staticExpr.replaceTypeInTree<Q>(replacement)
        val newRestOfMethodExpr = restOfMethodExpr.replaceTypeInTree<Q>(replacement)
        return RootExpression(
            staticExpr = newStaticExpr,
            restOfMethodExpr = newRestOfMethodExpr
        )
    }
}