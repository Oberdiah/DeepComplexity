package com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets

import com.github.oberdiah.deepcomplexity.evaluation.IExpr
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

/**
 * This is the set of possible values an expression can take, with their constraints.
 *
 * For example, after the code
 * ```
 * int x = 0;
 * if (a * 2 > 5 && b < 10) {
 *     x = 5;
 * }
 * ```
 * x would be a BundleSet with the following bundles:
 * [
 *     {bundle: {0}, constraints: {a > 5/2, b < 10}},
 *     {bundle: {5}, constraints: {a <= 5/2, b >= 10}}
 * ]
 *
 * Now, when an operation is performed between two BundleSets we effectively do an O(n^2) operation,
 * running each bundle against each other bundle.
 *
 * NOTE: The bundle's constraints are not necessarily mutually exclusive and should be considered
 * OR'd together. Take, for example,
 * ```
 * if (a > 5 || (a > 10 && b > 15)) {
 * ```
 */
class BundleSet<T : Any> private constructor(
    val ind: SetIndicator<T>,
    val bundles: List<ConstrainedVariances<T>>
) {
    companion object {
        fun <T : Any> empty(ind: SetIndicator<T>): BundleSet<T> {
            return BundleSet(ind, listOf())
        }

        fun <T : Any> unconstrainedBundle(bundle: Variances<T>): BundleSet<T> {
            return BundleSet(
                bundle.ind,
                listOf(
                    ConstrainedVariances.new(bundle, Constraints.completelyUnconstrained())
                )
            )
        }

        fun <T : Any> constrained(variances: Variances<T>, constraints: Constraints): BundleSet<T> {
            return BundleSet(
                variances.ind,
                listOf(
                    ConstrainedVariances.new(variances, constraints)
                )
            )
        }
    }

    @ConsistentCopyVisibility
    data class ConstrainedVariances<T : Any> private constructor(
        val variances: Variances<T>,
        val constraints: Constraints
    ) {
        companion object {
            fun <T : Any> new(variances: Variances<T>, constraints: Constraints): ConstrainedVariances<T> {
                return ConstrainedVariances(variances, constraints)
            }
        }

        override fun toString(): String {
            return toDebugString()
        }

        fun toDebugString(): String {
            if (constraints.isUnconstrained()) {
                return variances.toDebugString(constraints)
            }
            return "(${variances.toDebugString(constraints)} | $constraints)"
        }
    }

    override fun toString(): String {
        return toDebugString()
    }

    /**
     * Might return additional information beyond simply what the set contains.
     */
    fun toDebugString(): String {
        if (bundles.size == 1) {
            return bundles.first().toDebugString()
        }

        return bundles.joinToString {
            it.toDebugString()
        }
    }

    fun union(other: BundleSet<T>): BundleSet<T> {
        return BundleSet(ind, bundles.union(other.bundles).toList())
    }

    fun <Q : Any> unaryMapAndUnion(
        newInd: SetIndicator<Q>,
        op: (Variances<T>, Constraints) -> BundleSet<Q>
    ): BundleSet<Q> =
        BundleSet(newInd, bundles.flatMap { bundle ->
            op(bundle.variances, bundle.constraints).bundles.mapNotNull { newBundle ->
                val newConstraints = bundle.constraints.and(newBundle.constraints)

                if (newConstraints.unreachable) {
                    null
                } else {
                    ConstrainedVariances.new(newBundle.variances, newConstraints)
                }
            }
        })


    fun performUnaryOperation(op: (Variances<T>) -> Variances<T>): BundleSet<T> = unaryMap(ind, op)

    fun <Q : Any> unaryMap(newInd: SetIndicator<Q>, op: (Variances<T>) -> Variances<Q>): BundleSet<Q> {
        return BundleSet(newInd, bundles.map {
            ConstrainedVariances.new(op(it.variances), it.constraints)
        })
    }

    fun performBinaryOperation(
        other: BundleSet<T>,
        op: (Variances<T>, Variances<T>, Constraints) -> Variances<T>
    ): BundleSet<T> =
        binaryMap(ind, other, op)

    fun <Q : Any> binaryMap(
        newInd: SetIndicator<Q>,
        other: BundleSet<T>,
        op: (Variances<T>, Variances<T>, Constraints) -> Variances<Q>
    ): BundleSet<Q> {
        assert(ind == other.ind)

        val newBundles = mutableListOf<ConstrainedVariances<Q>>()
        for (myBundle in bundles) {
            for (otherBundle in other.bundles) {
                val newConstraints = myBundle.constraints.and(otherBundle.constraints)
                val newBundle = op(myBundle.variances, otherBundle.variances, newConstraints)

                if (newConstraints.unreachable) continue

                newBundles.add(ConstrainedVariances.new(newBundle, newConstraints))
            }
        }

        return BundleSet(newInd, newBundles)
    }

    /**
     * The expression you provide is converted into a set of constraints and then
     * combined with this bundle set.
     */
    fun constrainWith(constraintExpr: IExpr<Boolean>): BundleSet<T> {
        val constraints = ExprConstrain.getConstraints(constraintExpr)
        return BundleSet(ind, bundles.flatMap { bundle ->
            constraints.map { constraint ->
                ConstrainedVariances.new(bundle.variances, bundle.constraints.and(constraint))
            }
        })
    }

    /**
     * Collapses the full set of bundles into a single bundle, treating the constraints as an OR.
     *
     * @return A single bundle representing the collapsed state of the current bundle set.
     */
    fun collapse(): Bundle<T> {
        if (bundles.isEmpty()) {
            return ind.newEmptyBundle()
        }

        return bundles.fold(ind.newEmptyBundle()) { acc, bundle ->
            acc.union(bundle.variances.collapse(bundle.constraints))
        }
    }

    fun <Q : Any> cast(indicator: SetIndicator<Q>): BundleSet<Q>? {
        val cast = BundleSet(
            indicator, bundles.map {
                ConstrainedVariances.new(it.variances.cast(indicator) ?: return null, it.constraints)
            }
        )
        return cast
    }
}