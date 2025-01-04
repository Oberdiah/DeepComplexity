package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet.*
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import kotlin.reflect.KClass

class IfExpression(
    val trueExpr: Expr,
    val falseExpr: Expr,
    val condition: ExprRetBool,
) : Expr {
    override fun getCurrentlyUnresolved(): Set<UnresolvedExpression.Unresolved> {
        return trueExpr.getCurrentlyUnresolved() + falseExpr.getCurrentlyUnresolved() + condition.getCurrentlyUnresolved()
    }

    override fun getSetClass(): KClass<*> {
        return trueExpr.getSetClass()
    }

    override fun evaluate(): MoldableSet {
        val condition = condition.evaluate()
        return when (condition) {
            TRUE -> trueExpr.evaluate()
            FALSE -> falseExpr.evaluate()
            BOTH -> trueExpr.evaluate().union(falseExpr.evaluate())
            NEITHER -> throw IllegalStateException("Condition is neither true nor false! Something's wrong.")
        }
    }

    override fun toString(): String {
        return "($condition ? $trueExpr : $falseExpr)"
    }
}