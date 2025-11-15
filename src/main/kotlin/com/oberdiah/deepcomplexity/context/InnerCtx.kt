package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.evaluation.VarsExpr
import com.oberdiah.deepcomplexity.staticAnalysis.VarsMarker
import com.oberdiah.deepcomplexity.utilities.Utilities
import com.oberdiah.deepcomplexity.utilities.Utilities.betterPrependIndent

/**
 * Handles the combined static and dynamic variables in a context.
 *
 * Shouldn't care about any of what the expressions are or how they combine, should just be a thin
 * layer managing static expressions, dynamic expressions, and converting between them.
 */
class InnerCtx(
    val staticExpr: Expr<VarsMarker>,
    val dynamicVars: Vars,
) {
    companion object {
        fun new(): InnerCtx = InnerCtx(VarsExpr(), mapOf())
    }

    override fun toString(): String = staticExpr.toString()
        .lines()
        .joinToString("\n") { line ->
            if (line.contains(VarsExpr.STRING_PLACEHOLDER)) {
                val indent = "# " + line.takeWhile { c -> c.isWhitespace() }
                Utilities.varsToString(dynamicVars).betterPrependIndent(indent)
            } else {
                line
            }
        }

    val keys = staticExpr.iterateTree<VarsExpr>().flatMap { (it.vars ?: dynamicVars).keys }.toSet()

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

    fun forcedStatic(): InnerCtx = InnerCtx(
        staticExpr.replaceTypeInTree<VarsExpr> {
            if (it.vars != null) it else VarsExpr(dynamicVars)
        },
        mapOf()
    )

    fun forcedDynamic(getExpr: (Vars, UnknownKey) -> Expr<*>): InnerCtx = InnerCtx(
        VarsExpr(),
        keys.associateWith { key ->
            staticExpr.replaceTypeInLeaves<VarsExpr>(key.ind) {
                getExpr(it.vars ?: dynamicVars, key)
            }
        }
    )
}