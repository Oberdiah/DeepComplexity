package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

interface Bundle<T : Any> {
    val ind: SetIndicator<T>
    fun invert(): Bundle<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>?
    fun contains(element: T): Boolean
    fun isEmpty(): Boolean
    fun intersect(other: Bundle<T>): Bundle<T>
    fun union(other: Bundle<T>): Bundle<T>

    /**
     * Not a straightforward concept to grok.
     *
     * Converts this bundle from representing a specific constraint into representing a concrete set of values
     * that a variable may take. Essentially assign a meaningless blob of values to a specific context key.
     *
     * For example, the bundle (5..15) alone represents that range of integers, but when we convert it
     * (on, say, key `x`) that would still be (5..15) but under the hood the bundle knows that variation came
     * from the variable `x`. Down the line, arithmetic etc. performed on the bundle will know these values
     * are associated with key `x`, such that `x - x` will be 0.
     */
    // todo probably needs reworked/removed
    fun withVariance(key: Context.Key): Variances<T>
    fun toConstVariance(): Variances<T>
}