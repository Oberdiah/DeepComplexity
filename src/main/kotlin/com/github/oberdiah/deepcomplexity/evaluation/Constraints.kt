package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.Bundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.utilities.Functional

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
 * burdened with that information.
 */
class Constraints private constructor(
    // A key not in the map can be considered unconstrained, so an empty map is completely unconstrained.
    private val constraints: Map<Context.Key, Bundle<*>>,
) {
    /**
     * The constraints as a whole are unsatisfiable if any individual
     * constraint is unsatisfiable as the map of constraints acts as an AND.
     */
    val unreachable
        get() = constraints.any { it.value.isEmpty() }

    companion object {
        fun constrainedBy(constraints: Map<Context.Key, Bundle<*>>): Constraints {
            return Constraints(constraints)
        }

        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap())
        }

        fun unreachable(): Constraints {
            return Constraints(mapOf(Context.Key.EphemeralKey.new() to BooleanSet.NEITHER))
        }
    }

    override fun toString(): String {
        if (unreachable) return "unreachable"
        if (constraints.isEmpty()) return "unconstrained"
        return constraints.entries.joinToString("\n") { (key, bundle) ->
            "$key: $bundle"
        }
    }

    fun isUnconstrained(): Boolean {
        return constraints.isEmpty()
    }

    fun <T : Any> getConstraint(variable: VariableExpression<T>): Bundle<T>? {
        return constraints[variable.getKey().key]?.cast(variable.getSetIndicator())
    }

    fun <T : Any> getConstraint(ind: SetIndicator<T>, key: Context.Key): Bundle<T>? {
        return constraints[key]?.cast(ind)
    }

    fun addConstraint(key: Context.Key, expr: Bundle<*>): Constraints {
        return and(constrainedBy(mapOf(key to expr)))
    }

    fun invert(): Constraints {
        // You may be wondering why you aren't dealing with the non-map values?
        // I don't believe that's necessary, e.g., if (!(x > 5)) still doesn't constrain y.
        return Constraints(constraints.mapValues { (_, v) -> v.invert() })
    }

    /**
     * Merge two constraints — both sets of constraints must now be met.
     */
    fun and(other: Constraints): Constraints {
        if (unreachable) return this
        if (other.unreachable) return other

        val newConstraints = Functional.mergeMapsUnion(this.constraints, other.constraints) { lhs, rhs ->
            fun <T : Any> ugly(l: Bundle<T>): Bundle<T> =
                // Safety: We've asserted that the types are the same.
                @Suppress("UNCHECKED_CAST")
                l.intersect(rhs as Bundle<T>)

            assert(lhs.getIndicator() == rhs.getIndicator())

            ugly(lhs)
        }

        return constrainedBy(newConstraints)
    }
}