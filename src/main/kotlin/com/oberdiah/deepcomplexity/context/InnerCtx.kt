package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.evaluation.VarsExpr
import com.oberdiah.deepcomplexity.staticAnalysis.VarsMarker
import com.oberdiah.deepcomplexity.utilities.Utilities.betterPrependIndent

/**
 * Handles the combined static and dynamic variables in a context.
 *
 * Shouldn't care about any of what the expressions are or how they combine, should just be a thin
 * layer managing static expressions, dynamic expressions, and converting between them.
 */
class InnerCtx private constructor(
    private val staticExpr: Expr<VarsMarker>,
    val dynamicVars: Vars,
) {
    companion object {
        fun new(idx: ContextId): InnerCtx = InnerCtx(VarsExpr(), Vars(idx, mapOf()))

        fun combine(
            lhs: InnerCtx,
            rhs: InnerCtx,
            howStatic: (Expr<VarsMarker>, Expr<VarsMarker>) -> Expr<VarsMarker>,
            howDynamic: (Vars, Vars) -> Vars
        ): InnerCtx = InnerCtx(
            howStatic(lhs.staticExpr, rhs.staticExpr),
            howDynamic(lhs.dynamicVars, rhs.dynamicVars)
        )
    }

    override fun toString(): String = staticExpr.toString()
        .lines()
        .joinToString("\n") { line ->
            if (line.contains(VarsExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { c -> c.isWhitespace() }
                dynamicVars.toString().betterPrependIndent(indent)
            } else {
                line
            }
        }

    val keys: Set<UnknownKey> = staticExpr.iterateTree<VarsExpr>().flatMap { (it.vars ?: dynamicVars).keys }.toSet()

    fun mapDynamicVars(operation: (Vars) -> Vars): InnerCtx = InnerCtx(
        staticExpr,
        operation(dynamicVars)
    )

    fun mapStaticVars(operation: (Vars) -> Vars): InnerCtx = InnerCtx(
        staticExpr.replaceTypeInTree<VarsExpr> {
            if (it.vars != null) VarsExpr(operation(it.vars)) else it
        },
        dynamicVars
    )

    fun mapAllVars(operation: (Vars) -> Vars): InnerCtx = this.mapStaticVars(operation).mapDynamicVars(operation)

    fun forcedStatic(idx: ContextId): InnerCtx = InnerCtx(
        staticExpr.replaceTypeInTree<VarsExpr> {
            if (it.vars != null) it else VarsExpr(dynamicVars)
        },
        Vars.new(idx)
    )

    fun forcedDynamic(idx: ContextId): InnerCtx = InnerCtx(
        VarsExpr(),
        Vars(idx, keys.associateWith { key ->
            staticExpr.replaceTypeInLeaves<VarsExpr>(key.ind) {
                (it.vars ?: dynamicVars).get(key)
            }
        })
    )
}