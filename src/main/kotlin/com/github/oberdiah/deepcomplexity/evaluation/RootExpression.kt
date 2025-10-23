package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator

/**
 * In order to implement early returns, each expression in a context needs to be
 * split in half - a section representing the expression currently in flux and a section
 * that is set in stone and can no longer be changed.
 *
 * In the [staticExpr], [DynamicExpr]s are used to indicate the sections that should
 * be replaced with the [dynamicExpr] when stacking.
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
 * staticExpr: (y > 10) ? (a.x` + 1) : Dyn
 * dynamicExpr: a.x` + 2
 * ```
 */
class RootExpression<T : Any>(
    private val staticExpr: Expr<*>,
    private val dynamicExpr: Expr<T>,
) {
    companion object {
        /**
         * Should be used pretty sparingly.
         */
        fun new(expr: Expr<*>): RootExpression<*> {
            return RootExpression(
                staticExpr = DynamicExpr(expr.ind),
                dynamicExpr = expr
            )
        }

        fun combine(
            doNothingExpr: Expr<*>,
            lhs: RootExpression<*>?,
            rhs: RootExpression<*>?,
            how: (a: Expr<*>, b: Expr<*>) -> Expr<*>
        ): RootExpression<*> {
            val ind = doNothingExpr.ind

            val lhsStaticExpr = lhs?.staticExpr ?: DynamicExpr(ind)
            val rhsStaticExpr = rhs?.staticExpr ?: DynamicExpr(ind)

            val finalStaticExpr = how(lhsStaticExpr, rhsStaticExpr)

            val rhsDynExpr = rhs?.dynamicExpr ?: doNothingExpr
            val lhsDynExpr = lhs?.dynamicExpr ?: doNothingExpr

            val finalDynExpr = how(lhsDynExpr, rhsDynExpr)

            return RootExpression(
                staticExpr = finalStaticExpr,
                dynamicExpr = finalDynExpr
            )
        }
    }

    override fun toString(): String = if (staticExpr is DynamicExpr<*>) {
        dynamicExpr.toString()
    } else {
        "{\n${staticExpr.toString().prependIndent()}\n\n${dynamicExpr.toString().prependIndent()}\n}"
    }

    private val ind: SetIndicator<T> = dynamicExpr.ind

    fun withStackedRoot(other: RootExpression<*>?): RootExpression<*> {
        if (other == null) return this

        return RootExpression(
            staticExpr = staticExpr.replaceTypeInTree<DynamicExpr<*>> {
                other.staticExpr
            },
            dynamicExpr = this.dynamicExpr
        )
    }

    fun stackedUnder(stackedUnder: RootExpression<*>): RootExpression<*> {
        return RootExpression(
            staticExpr = staticExpr.replaceTypeInTree<DynamicExpr<*>> {
                stackedUnder.staticExpr
            },
            dynamicExpr = stackedUnder.dynamicExpr
        )
    }

    /**
     * Returns the 'dynamic' part of this expression; this is the bit you want when you getVar().
     */
    fun getDynExpr(): Expr<*> = dynamicExpr

    /**
     * You'll typically only call this if you're a method and want to make the whole thing dynamic again
     * as short-circuiting returns are no longer a concern. Collapses the static and dynamic parts.
     * For example,
     * ```
     * staticExpr: (y > 10) ? (a.x` + 1) : Dyn
     * dynamicExpr: a.x` + 2
     * ```
     * becomes
     * ```
     * staticExpr: Dyn
     * dynamicExpr: (y > 10) ? (a.x` + 1) : a.x` + 2
     * ```
     */
    fun forcedDynamic(): RootExpression<*> = RootExpression(
        staticExpr = DynamicExpr(ind),
        dynamicExpr = staticExpr.replaceTypeInTree<DynamicExpr<*>> {
            dynamicExpr
        }
    )

    /**
     * The opposite of [forcedDynamic]; collapses the static and dynamic parts together and makes
     * them both static. Used when a control flow break has been reached, e.g. a return statement.
     */
    fun forcedStatic(doNothingExpr: Expr<*>): RootExpression<*> = RootExpression(
        staticExpr = staticExpr.replaceTypeInTree<DynamicExpr<*>> {
            dynamicExpr
        },
        dynamicExpr = doNothingExpr
    )

    fun optimise() = RootExpression(
        staticExpr = staticExpr.optimise(),
        dynamicExpr = dynamicExpr.optimise()
    )

    internal inline fun <reified Q> replaceTypeInTree(crossinline replacement: (Q) -> Expr<*>?): RootExpression<T> =
        RootExpression(
            staticExpr = staticExpr.replaceTypeInTree<Q>(replacement),
            dynamicExpr = dynamicExpr.replaceTypeInTree<Q>(replacement)
        )
}