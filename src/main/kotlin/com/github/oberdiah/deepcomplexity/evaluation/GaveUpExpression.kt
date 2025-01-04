package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.GenericSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.MoldableSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet
import kotlin.reflect.KClass

class GaveUpExpression(private val weWouldCallIfWeKnewWhatToDo: Expr) : Expr {
    override fun getCurrentlyUnresolved(): Set<UnresolvedExpression.Unresolved> {
        return setOf()
    }

    override fun getSetClass(): KClass<*> {
        return weWouldCallIfWeKnewWhatToDo.getSetClass()
    }

    override fun evaluate(): MoldableSet {
        return when (weWouldCallIfWeKnewWhatToDo.getSetClass()) {
            NumberSet::class -> NumberSet.gaveUp()
            BooleanSet::class -> BooleanSet.BOTH
            GenericSet::class -> TODO("Not implemented yet")
            else -> throw IllegalStateException("Unknown set class")
        }
    }
}