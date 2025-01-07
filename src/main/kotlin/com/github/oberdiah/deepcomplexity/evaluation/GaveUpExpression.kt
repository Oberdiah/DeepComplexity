package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

object GaveUpExpression {
    fun fromExpr(expr: IExpr): IExpr {
        return when (ExprClass.getSetClass(expr)) {
            NumberSet::class -> ConstExprNum(
                NumberSet.fullRange(ExprClass.getBaseClass(expr))
            )

            BooleanSet::class -> ConstExprBool(BooleanSet.BOTH)
            GenericSet::class -> ConstExprGeneric(GenericSet.everyValue())
            else -> throw IllegalArgumentException("Unknown set class")
        }
    }
}