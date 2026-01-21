package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.LValueKey
import com.oberdiah.deepcomplexity.evaluation.VarsExpr
import com.oberdiah.deepcomplexity.staticAnalysis.VarsMarker
import com.oberdiah.deepcomplexity.utilities.Utilities.betterPrependIndent

/**
 * Handles the combined static and dynamic variables in a context.
 *
 * Shouldn't care about any of what the expressions are or how they combine, should just be a thin
 * layer managing the static and dynamic expressions.
 *
 * The [staticExpr] is an expression with information about return values we've encountered, and the [dynamicVars] are
 * in effect the 'current' state of the variables that we modify as we continue to process the program.
 *
 * For example, the code
 * ```
 * y = 0;
 * if (x > 5) {
 *  return;
 * }
 * y += 1;
 * ```
 * would result in an [InnerCtx] that looks like:
 * ```
 * staticExpr:
 *  if (x > 5) {
 *      {
 *          y: 0
 *      }
 *  } else {
 *      ##DynamicVars##
 *  }
 * dynamicVars:
 *  {
 *      y: 1
 *  }
 * ```
 *
 * If [dynamicVars] is null, the static expression does not have any dynamic variables to link to.
 */
class InnerCtx private constructor(
    private val staticExpr: Expr<VarsMarker>,
    private val dynamicVars: Vars?,
) {
    companion object {
        fun new(): InnerCtx = InnerCtx(VarsExpr.new(), Vars.new())

        fun combine(
            lhs: InnerCtx,
            rhs: InnerCtx,
            howStatic: (Expr<VarsMarker>, Expr<VarsMarker>) -> Expr<VarsMarker>,
            howDynamic: (Vars, Vars) -> Vars
        ): InnerCtx = InnerCtx(
            howStatic(lhs.staticExpr, rhs.staticExpr),
            when {
                lhs.dynamicVars == null -> rhs.dynamicVars
                rhs.dynamicVars == null -> lhs.dynamicVars
                else -> howDynamic(lhs.dynamicVars, rhs.dynamicVars)
            }
        )
    }

    init {
        if (dynamicVars == null) {
            require(staticExpr.iterateTree<VarsExpr>().none { it.isDynamic }) {
                "Static expression contains dynamic references, but dynamicVars is null!"
            }
        }
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

    /**
     * Returns the dynamic variables in this inner context. Returns a new Vars object if one is not possible.
     */
    fun grabDynamicVars(): Vars? = dynamicVars

    fun resolveUsing(vars: Vars): InnerCtx = InnerCtx(
        vars.resolveKnownVariables(staticExpr),
        dynamicVars?.resolveUsing(vars)
    )

    val keys: Set<UnknownKey> = staticExpr.iterateTree<VarsExpr>().flatMap {
        getVarsFromVarsExpr(it).keys
    }.toSet()

    fun mapDynamicVars(operation: (Vars) -> Vars): InnerCtx = InnerCtx(
        staticExpr,
        dynamicVars?.let { operation(it) }
    )

    fun mapStaticVars(operation: (Vars) -> Vars): InnerCtx = InnerCtx(
        staticExpr.rewriteTypeInTreeSameType<VarsExpr> { it.map(operation) },
        dynamicVars
    )

    fun mapAllVars(operation: (Vars) -> Vars): InnerCtx = this.mapStaticVars(operation).mapDynamicVars(operation)

    fun forcedStatic(): InnerCtx = InnerCtx(
        staticExpr.rewriteTypeInTreeSameType<VarsExpr> {
            // !! is safe by init {} check.
            if (it.isDynamic) VarsExpr.new(VarsExpr.DynamicOrStatic.Static(dynamicVars!!)) else it
        },
        null
    )

    fun forcedDynamic(): InnerCtx = InnerCtx(
        VarsExpr.new(),
        Vars(keys.associateWith { key ->
            staticExpr.rewriteTypeInTree<VarsExpr> {
                getVarsFromVarsExpr(it).get(LValueKey.new(key))
            }
        })
    )

    private fun getVarsFromVarsExpr(varsExpr: VarsExpr): Vars {
        return when (varsExpr.vars) {
            is VarsExpr.DynamicOrStatic.Static -> varsExpr.vars.vars
            // The init check should catch most cases of the exception - it could only be thrown
            // if somehow called with a varsExpr that didn't come from our static expression.
            is VarsExpr.DynamicOrStatic.Dynamic -> dynamicVars ?: throw IllegalStateException("Dynamic vars is null!")
        }
    }
}