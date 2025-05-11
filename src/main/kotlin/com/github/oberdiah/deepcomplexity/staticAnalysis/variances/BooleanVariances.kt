package com.github.oberdiah.deepcomplexity.staticAnalysis.variances

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets.Constraints
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.BooleanBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.Bundle

class BooleanVariances(private val value: BooleanBundle) : Variances<Boolean> {
    fun invert(): BooleanVariances = BooleanVariances(value.invert())
    override val ind: SetIndicator<Boolean> = BooleanSetIndicator

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>? =
        throw IllegalArgumentException("Cannot cast boolean to $newInd")

    override fun collapse(constraints: Constraints): Bundle<Boolean> = value

    override fun toDebugString(constraints: Constraints): String = value.toString()

    fun booleanOperation(other: BooleanVariances, operation: BooleanOp): BooleanVariances {
        return BooleanVariances(value.booleanOperation(other.value, operation))
    }
}