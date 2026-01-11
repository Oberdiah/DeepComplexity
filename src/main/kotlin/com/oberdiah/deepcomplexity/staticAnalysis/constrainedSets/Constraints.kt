package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.evaluation.Key
import com.oberdiah.deepcomplexity.evaluation.ExprEvaluate
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.sets.BooleanSet
import com.oberdiah.deepcomplexity.staticAnalysis.sets.ISet
import com.oberdiah.deepcomplexity.utilities.Functional
import com.oberdiah.deepcomplexity.utilities.Utilities.WONT_IMPLEMENT
import kotlin.test.assertIsNot

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
    val constraints: Map<Key, ISet<*>>,
) {
    /**
     * The constraints as a whole are unsatisfiable if any individual
     * constraint is unsatisfiable as the map of constraints acts as an AND.
     */
    val unreachable
        get() = constraints.any { it.value.isEmpty() }

    companion object {
        fun constrainedBy(constraints: Map<Key, ISet<*>>): Constraints {
            return Constraints(constraints)
        }

        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap())
        }

        fun unreachable(): Constraints {
            return Constraints(mapOf(Key.ConstantKey to BooleanSet.NEITHER))
        }
    }

    override fun toString(): String {
        if (unreachable) return "unreachable"
        if (constraints.isEmpty()) return "unconstrained"
        return constraints.entries.joinToString(" ") { (key, bundle) ->
            "$key[$bundle]"
        }
    }

    fun reduceAndSimplify(scope: ExprEvaluate.Scope): Constraints {
        return Constraints(constraints.filterKeys { scope.shouldKeep(it) })
    }

    fun isUnconstrained(): Boolean {
        return constraints.isEmpty()
    }

    fun <T : Any> getConstraint(ind: Indicator<T>, key: Key): ISet<T> {
        return constraints[key]?.cast(ind) ?: ind.newFullSet()
    }

    fun withConstraint(key: Key, iSet: ISet<*>): Constraints {
        assertIsNot<Key.ConstantKey>(
            key,
            "Constant keys shouldn't be allowed to be added to constraints."
        )
        assert(key.ind == iSet.ind) {
            "Key and bundle must have the same type. (${key.ind} != ${iSet.ind})"
        }
        return and(constrainedBy(mapOf(key to iSet)))
    }

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
                // Safety: We've asserted that the types are the same.
                @Suppress("UNCHECKED_CAST")
                l.intersect(rhs as ISet<T>)

            assert(lhs.ind == rhs.ind)

            ugly(lhs)
        }

        return constrainedBy(newConstraints)
    }
}