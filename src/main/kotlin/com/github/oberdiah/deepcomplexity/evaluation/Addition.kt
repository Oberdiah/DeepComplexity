package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSet

class Addition(
    val lhs: Expression<NumberSet>,
    val rhs: Expression<NumberSet>
) : Expression<NumberSet> {
    override fun evaluate(): NumberSet {
        val lhs = lhs.evaluate()
        val rhs = rhs.evaluate()

        return lhs.addition(rhs)
    }
}