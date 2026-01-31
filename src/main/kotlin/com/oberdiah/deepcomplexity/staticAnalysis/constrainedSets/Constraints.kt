package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.utilities.Functional
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT

/**
 * All constraints in the map must be true, it in-effect acts as an AND.
 *
 * To explain why this is a map, take this example:
 * ```
 * int hello = 0;
 * if (a * 2 > 5 && b < 10) {
 *     // do foo
 *     hello = 5;
 * }
 * ```
 * In this case, we know a > 2 and b < 10, and `hello` equaling 5 has to be
 * burdened with that information, despite the fact that `hello` is not
 * actually used in the condition.
 */
@ConsistentCopyVisibility
data class Constraints private constructor(
    // A key not in the map can be considered unconstrained, so an empty map is completely unconstrained.
    val constraints: Map<EvaluationKey, ISet<*>>,
) {
    init {
        require(constraints.size < 10) {
            "No reason for 10 to be the upper bound, but I thought I ought to know about it. Found: ${constraints.size}"
        }
    }

    /**
     * The constraints as a whole are unsatisfiable if any individual
     * constraint is unsatisfiable as the map of constraints acts as an AND.
     */
    val unreachable
        get() = constraints.any { it.value.isEmpty() }

    companion object {
        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap())
        }

        fun unreachable(): Constraints {
            return Constraints(mapOf(EvaluationKey.ConstantKey to BooleanSet.NEITHER))
        }
    }

    override fun toString(): String {
        if (unreachable) return "unreachable"
        if (constraints.isEmpty()) return "unconstrained"
        return constraints.entries.joinToString(" ") { (key, bundle) ->
            "$key[$bundle]"
        }
    }

    fun onlyConstraining(keys: Set<EvaluationKey>): Constraints {
        return Constraints(constraints.filterKeys { it in keys })
    }

    fun isUnconstrained(): Boolean {
        return constraints.isEmpty()
    }

    fun <T : Any> getConstraint(ind: Indicator<T>, key: EvaluationKey): ISet<T> {
        return constraints[key]?.cast(ind) ?: ind.newFullSet()
    }

    fun withConstraint(key: EvaluationKey, iSet: ISet<*>): Constraints {
        require(key !is EvaluationKey.ConstantKey) {
            "Constant keys shouldn't be allowed to be added to constraints."
        }
        require(key.ind == iSet.ind) {
            "Key and bundle must have the same type. (${key.ind} != ${iSet.ind})"
        }
        return and(Constraints(mapOf(key to iSet)))
    }

    @Suppress("unused")
    fun invert(): Constraints {
        // Inverting a Constraints is too risky; both constraints and sets operate
        // on a best-effort basis with a bubble of uncertainty. Values outside a set are guaranteed
        // to not be in the set, but the inverse is not true. Inverting a set while maintaining this
        // invariant is impossible.
        WONT_IMPLEMENT()
    }

    /**
     * Merge two constraints — both sets of constraints must now be met.
     */
    fun and(other: Constraints): Constraints {
        if (unreachable) return this
        if (other.unreachable) return other

        val newConstraints = Functional.mergeMapsUnion(this.constraints, other.constraints) { lhs, rhs ->
            fun <T : Any> ugly(l: ISet<T>): ISet<T> =
                // Safety: We've required that the types are the same.
                @Suppress("UNCHECKED_CAST")
                l.intersect(rhs as ISet<T>)

            require(lhs.ind == rhs.ind)

            ugly(lhs)
        }

        return Constraints(newConstraints)
    }
}