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
class RootExpression<T : Any>(private val staticExpr: Expr<*>, private val restOfMethodExpr: Expr<T>) {
    companion object {
        fun new(expr: Expr<*>): RootExpression<*> {
            return RootExpression(
                staticExpr = RestOfMethodExpr,
                restOfMethodExpr = expr
            )
        }

        fun combine(
            lhs: RootExpression<*>?,
            rhs: RootExpression<*>?,
            how: (a: Expr<*>, b: Expr<*>) -> Expr<*>
        ): RootExpression<*> {
            TODO()
        }
    }

    fun getExpr(): Expr<*> {
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
}