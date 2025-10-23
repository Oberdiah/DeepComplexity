package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

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
    private val restOfMethodExpr: Expr<T>,
) {
    companion object {
        /**
         * Should be used pretty sparingly. The majority of the time using [withREMExpr] makes more sense.
         */
        fun new(expr: Expr<*>): RootExpression<*> {
            return RootExpression(
                staticExpr = RestOfMethodExpr(expr.ind),
                restOfMethodExpr = expr
            )
        }

        fun combine(
            doNothingExpr: Expr<*>,
            lhs: RootExpression<*>?,
            rhs: RootExpression<*>?,
            how: (a: Expr<*>, b: Expr<*>) -> Expr<*>
        ): RootExpression<*> {
            val ind = doNothingExpr.ind

            val lhsStaticExpr = lhs?.staticExpr ?: RestOfMethodExpr(ind)
            val rhsStaticExpr = rhs?.staticExpr ?: RestOfMethodExpr(ind)

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

    override fun toString(): String {
        if (staticExpr is RestOfMethodExpr<*>) {
            return restOfMethodExpr.toString()
        }

        return "{\n${staticExpr.toString().prependIndent()}\n\n${restOfMethodExpr.toString().prependIndent()}\n}"
    }

    private val ind: SetIndicator<T> = restOfMethodExpr.ind

    fun withStackedRoot(other: RootExpression<*>?): RootExpression<*> {
        if (other == null) return this

        return RootExpression(
            staticExpr = staticExpr.replaceTypeInTree<RestOfMethodExpr<*>> {
                other.staticExpr
            },
            restOfMethodExpr = this.restOfMethodExpr
        )
    }

    fun withHitReturnMethod(doNothingExpr: Expr<*>): RootExpression<*> {
        return RootExpression(
            staticExpr = staticExpr.replaceTypeInTree<RestOfMethodExpr<*>> {
                restOfMethodExpr
            },
            restOfMethodExpr = doNothingExpr
        )
    }

    /**
     * Returns the 'rest of method' of this expression; this is the bit you typically want when you getVar().
     */
    fun getREMExpr(): Expr<*> = restOfMethodExpr


    /**
     * The opposite of [getREMExpr]; returns a new RootExpression with the given REM expression.
     */
    fun withREMExpr(expr: Expr<*>): RootExpression<*> = RootExpression(
        staticExpr = staticExpr,
        restOfMethodExpr = expr
    )

    /**
     * You'll typically only call this if you're a method and want to collapse this expression
     * as short-circuiting returns are no longer a concern.
     */
    fun collapse(): RootExpression<*> =
        RootExpression(
            RestOfMethodExpr(ind),
            staticExpr.replaceTypeInTree<RestOfMethodExpr<*>> {
                restOfMethodExpr
            }
        )

    fun optimise() = RootExpression(
        staticExpr = staticExpr.optimise(),
        restOfMethodExpr = restOfMethodExpr.optimise()
    )

    internal inline fun <reified Q> replaceTypeInTree(crossinline replacement: (Q) -> Expr<*>?): RootExpression<T> {
        val newStaticExpr = staticExpr.replaceTypeInTree<Q>(replacement)
        val newRestOfMethodExpr = restOfMethodExpr.replaceTypeInTree<Q>(replacement)
        return RootExpression(
            staticExpr = newStaticExpr,
            restOfMethodExpr = newRestOfMethodExpr
        )
    }
}