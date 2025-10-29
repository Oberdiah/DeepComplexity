package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

interface ISet<T : Any> {
    val ind: SetIndicator<T>

    /**
     * The number of elements in the set, if it can be easily computed.
     * Otherwise, null.
     */
    fun size(): Long?
    fun invert(): ISet<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>?
    fun contains(element: T): Boolean
    fun isEmpty(): Boolean

    /**
     * True if the set is full, meaning it contains all possible values.
     */
    fun isFull(): Boolean
    fun intersect(other: ISet<T>): ISet<T>
    fun union(other: ISet<T>): ISet<T>
    fun toConstVariance(): Variances<T>

    /**
     * Note: Only Numbers need to worry about handling [comparisonOp]s that aren't equality or inequality,
     * and can throw if they receive one.
     *
     * How this comparison works is slightly unintuitive. To resolve to True or False, there must be no situation
     * in which the event could occur. For equality, for example, two identical sets {5, 6} and {5, 6}
     * are NOT equal, nor are they unequal.
     *
     * {5} & {5} = True
     * {5} & {6} = False
     * {5, 6} & {5, 6} = Neither
     * {5, 6} & {7, 8} = False
     * {5, 6} & {5} = Neither
     *
     * Effectively, for equality you return false iff the sets are disjoint, true iff they are both size 1 and
     * equal to each other, and neither otherwise.
     */
    fun comparisonOperation(other: ISet<T>, operation: ComparisonOp): BooleanSet {
        // A default implementation for non-numeric sets that just handles EQ and NE
        if (operation != ComparisonOp.EQUAL && operation != ComparisonOp.NOT_EQUAL) {
            throw IllegalArgumentException("Only EQ and NE are supported for non-numeric sets")
        }

        val intersection = this.intersect(other)

        val areEqual = when {
            intersection.isEmpty() -> BooleanSet.FALSE
            this.size() == 1L && other.size() == 1L && intersection.size() == 1L -> BooleanSet.TRUE
            else -> BooleanSet.NEITHER
        }

        return when (operation) {
            ComparisonOp.EQUAL -> areEqual
            ComparisonOp.NOT_EQUAL -> areEqual.booleanInvert()
            else -> throw IllegalArgumentException("Only EQ and NE are supported for non-numeric sets")
        }
    }
}