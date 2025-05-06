package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.ExprConstrain
import com.github.oberdiah.deepcomplexity.evaluation.IExpr
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

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
    val bundles: List<ConstrainedBundle<T>>
) {
    companion object {
        fun <T : Any> empty(ind: SetIndicator<T>): BundleSet<T> {
            return BundleSet(
                ind,
                listOf(
//                    ConstrainedBundle(ind.newEmptyBundle(), Constraints.completelyUnconstrained())
                )
            )
        }

        fun <T : Any> unconstrainedBundle(bundle: Bundle<T>): BundleSet<T> {
            return BundleSet(
                bundle.getIndicator(),
                listOf(
                    ConstrainedBundle(bundle, Constraints.completelyUnconstrained())
                )
            )
        }

        fun <T : Any> constrained(bundle: Bundle<T>, constraints: Constraints): BundleSet<T> {
            return BundleSet(
                bundle.getIndicator(),
                listOf(
                    ConstrainedBundle(bundle, constraints)
                )
            )
        }

        fun <T : Any> constrained(bundle: Bundle<T>, constraints: List<Constraints>): BundleSet<T> = BundleSet(
            bundle.getIndicator(),
            constraints.map { ConstrainedBundle(bundle, it) }
        )
    }

    data class ConstrainedBundle<T : Any>(val bundle: Bundle<T>, val constraints: Constraints) {
        fun toDebugString(): String {
            if (constraints.isUnconstrained()) {
                return "$bundle"
            }
            return "($bundle | $constraints)"
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

    fun <Q : Any> unaryMapAndUnion(newInd: SetIndicator<Q>, op: (Bundle<T>) -> BundleSet<Q>): BundleSet<Q> =
        BundleSet(newInd, bundles.flatMap { bundle ->
            op(bundle.bundle).bundles.mapNotNull { newBundle ->
                val newConstraints = bundle.constraints.and(newBundle.constraints)

                if (newConstraints.unreachable) {
                    null
                } else {
                    ConstrainedBundle(newBundle.bundle, newConstraints)
                }
            }
        })


    fun performUnaryOperation(op: (Bundle<T>) -> Bundle<T>): BundleSet<T> = unaryMap(ind, op)

    fun <Q : Any> unaryMap(newInd: SetIndicator<Q>, op: (Bundle<T>) -> Bundle<Q>): BundleSet<Q> {
        return BundleSet(newInd, bundles.map {
            ConstrainedBundle(op(it.bundle), it.constraints)
        })
    }

    fun performBinaryOperation(other: BundleSet<T>, op: (Bundle<T>, Bundle<T>) -> Bundle<T>): BundleSet<T> =
        binaryMap(ind, other, op)

    fun <Q : Any> binaryMap(
        newInd: SetIndicator<Q>,
        other: BundleSet<T>,
        op: (Bundle<T>, Bundle<T>) -> Bundle<Q>
    ): BundleSet<Q> {
        assert(ind == other.ind)

        val newBundles = mutableListOf<ConstrainedBundle<Q>>()
        for (myBundle in bundles) {
            for (otherBundle in other.bundles) {
                val newConstraints = myBundle.constraints.and(otherBundle.constraints)
                val newBundle = op(myBundle.bundle, otherBundle.bundle)

                if (newConstraints.unreachable) continue

                newBundles.add(ConstrainedBundle(newBundle, newConstraints))
            }
        }

        return BundleSet(newInd, newBundles)
    }

    fun constrainWith(constraints: Constraints): BundleSet<T> {
        return BundleSet(ind, bundles.map {
            ConstrainedBundle(it.bundle, it.constraints.and(constraints))
        })
    }

    /**
     * The expression you provide is converted into a set of constraints and then
     * combined with this bundle set.
     */
    fun constrainWith(constraintExpr: IExpr<Boolean>): BundleSet<T> {
        val constraints = ExprConstrain.getConstraints(constraintExpr)
        return BundleSet(ind, bundles.flatMap { bundle ->
            constraints.map { constraint ->
                ConstrainedBundle(bundle.bundle, bundle.constraints.and(constraint))
            }
        })
    }

    fun intersect(other: BundleSet<T>): BundleSet<T> {
        return performBinaryOperation(other) { myBundle, otherBundle ->
            myBundle.intersect(otherBundle)
        }
    }

    /**
     * Inverts all the contained bundles.
     */
    fun invert(): BundleSet<T> {
        return performUnaryOperation { it.invert() }
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

        return bundles.map { it.bundle }.reduce { acc, bundle ->
            acc.union(bundle)
        }
    }

    fun <Q : Any> cast(indicator: SetIndicator<Q>): BundleSet<Q>? {
        return BundleSet(
            indicator, bundles.map {
                ConstrainedBundle(it.bundle.cast(indicator) ?: return null, it.constraints)
            }
        )
    }
}