package com.oberdiah.deepcomplexity.staticAnalysis.variances

import com.oberdiah.deepcomplexity.context.Key
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Constraints
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into

/**
 * A set of possible values of type T, alongside optional variance data that can be used to
 * track the values as operations are performed on them.
 */
interface Variances<T : Any> {
    val ind: SetIndicator<T>
    fun <Q : Any> cast(newInd: SetIndicator<Q>, constraints: Constraints): Variances<Q>?
    fun collapse(constraints: Constraints): ISet<T>
    fun toDebugString(constraints: Constraints): String

    /**
     * Returns true if this variance is currently tracking the given key, i.e. the variance would
     * return a different value if that key was constrained.
     */
    fun varsTracking(): Collection<Key>
    fun reduceAndSimplify(scope: ExprEvaluate.Scope, constraints: Constraints): Variances<T>

    /**
     * Note: Only Numbers need to worry about handling [comparisonOp]s that aren't equality or inequality,
     * all other types can throw if they receive one.
     *
     * At the very least, there are improvements to this that could be made for numbers.
     * I wrote the above down in a previous comment, but there was no further information
     * so I couldn't tell you specifically what those improvements might be.
     */
    fun comparisonOperation(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        constraints: Constraints
    ): BooleanVariances =
        collapse(constraints)
            .comparisonOperation(other.collapse(constraints), comparisonOp)
            .toConstVariance()
            .into()

    /**
     * Note: Only Numbers need to worry about handling [comparisonOp]s that aren't equality or inequality,
     * all other types can throw if they receive one.
     */
    fun generateConstraintsFrom(
        other: Variances<T>,
        comparisonOp: ComparisonOp,
        incomingConstraints: Constraints
    ): Constraints
}