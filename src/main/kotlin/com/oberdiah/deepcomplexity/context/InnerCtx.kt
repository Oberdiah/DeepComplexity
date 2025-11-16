package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.ExpressionExtensions.replaceTypeInLeaves
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
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
class InnerCtx private constructor(
    private val idx: ContextId,
    private val staticExpr: Expr<VarsMarker>,
    val dynamicVars: Vars,
) {
    companion object {
        fun new(idx: ContextId): InnerCtx = init(idx, VarsExpr(), mapOf())

        fun combine(
            idx: ContextId,
            lhs: InnerCtx,
            rhs: InnerCtx,
            howStatic: (Expr<VarsMarker>, Expr<VarsMarker>) -> Expr<VarsMarker>,
            howDynamic: (Vars, Vars) -> Vars
        ): InnerCtx = init(
            idx,
            howStatic(lhs.staticExpr, rhs.staticExpr),
            howDynamic(lhs.dynamicVars, rhs.dynamicVars)
        )

        /**
         * Only this should call the base constructor.
         */
        fun init(idx: ContextId, staticExpr: Expr<VarsMarker>, dynamicVars: Vars): InnerCtx {
            val variables: Vars = dynamicVars.mapValues { expr ->
                expr.value.replaceTypeInTree<VariableExpr<*>> {
                    VariableExpr.new(it.key.withAddedContextId(idx))
                }
            }.mapKeys { it.key.withAddedContextId(idx) }

            require(variables.keys.filterIsInstance<ReturnKey>().size <= 1) {
                "A context cannot have multiple return keys."
            }

            return InnerCtx(idx, staticExpr, variables)
        }
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

    fun mapDynamicVars(operation: (Vars) -> Vars): InnerCtx = init(
        idx,
        staticExpr,
        operation(dynamicVars)
    )

    fun mapStaticVars(operation: (Vars) -> Vars): InnerCtx = init(
        idx,
        staticExpr.replaceTypeInTree<VarsExpr> {
            if (it.vars != null) VarsExpr(operation(it.vars)) else it
        },
        dynamicVars
    )

    fun mapAllVars(operation: (Vars) -> Vars): InnerCtx = this.mapStaticVars(operation).mapDynamicVars(operation)

    fun forcedStatic(): InnerCtx = init(
        idx,
        staticExpr.replaceTypeInTree<VarsExpr> {
            if (it.vars != null) it else VarsExpr(dynamicVars)
        },
        mapOf()
    )

    fun forcedDynamic(getExpr: (Vars, UnknownKey) -> Expr<*>): InnerCtx = init(
        idx,
        VarsExpr(),
        keys.associateWith { key ->
            staticExpr.replaceTypeInLeaves<VarsExpr>(key.ind) {
                getExpr(it.vars ?: dynamicVars, key)
            }
        }
    )
}