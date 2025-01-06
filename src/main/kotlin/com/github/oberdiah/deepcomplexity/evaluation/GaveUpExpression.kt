package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

class GaveUpExpression(private val weWouldCallIfWeKnewWhatToDo: IExpr) : IExpr {
    override fun getVariables(resolved: Boolean): Set<VariableExpression> {
        return setOf()
    }

    override fun getSetClass(): KClass<*> {
        return weWouldCallIfWeKnewWhatToDo.getSetClass()
    }

    override fun evaluate(condition: IExprRetBool): IMoldableSet {
        return when (weWouldCallIfWeKnewWhatToDo.getSetClass()) {
            NumberSet::class -> NumberSet.gaveUp()
            BooleanSet::class -> BooleanSet.BOTH
            GenericSet::class -> TODO("Not implemented yet")
            else -> throw IllegalStateException("Unknown set class")
        }
    }

    override fun toString(): String {
        return "X"
    }
}