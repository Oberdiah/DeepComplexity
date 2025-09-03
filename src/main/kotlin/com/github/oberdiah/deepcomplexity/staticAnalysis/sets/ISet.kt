package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

interface ISet<T : Any> {
    val ind: SetIndicator<T>
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
     */
    fun comparisonOperation(other: ISet<T>, operation: ComparisonOp): BooleanSet
}