package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.ConstrainedSet
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
    // A key that isn't in the map can be considered unconstrained, so an empty map is completely unconstrained.
    private val constraints: Map<Context.Key, ConstrainedSet<*>>,
    // If this is true these constraints have been proven to be unsatisfiable.
    // Currently, this is the constraint you get when you do something like `if (false) { ... }`
    private val unreachable: Boolean
) {
    companion object {
        fun constrainedBy(constraints: Map<Context.Key, ConstrainedSet<*>>): Constraints {
            return Constraints(constraints, false)
        }

        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap(), false)
        }

        fun unreachable(): Constraints {
            return Constraints(emptyMap(), true)
        }
    }

    fun <T : ConstrainedSet<T>> getConstraint(variable: VariableExpression<T>): T? {
        return constraints[variable.getKey().key]?.cast(variable.getSetIndicator())
    }

    fun <T : ConstrainedSet<T>> getConstraint(ind: SetIndicator<T>, key: Context.Key): T? {
        return constraints[key]?.cast(ind)
    }

    fun addConstraint(key: Context.Key, expr: ConstrainedSet<*>): Constraints {
        return and(constrainedBy(mapOf(key to expr)))
    }

    fun invert(): Constraints {
        if (unreachable) return completelyUnconstrained()

        // You may be wondering why aren't you dealing with the non-map values?
        // I don't believe that's necessary, e.g. if (!(x > 5)) still doesn't constrain y.
        return Constraints(constraints.mapValues { (_, v) -> v.invert() }, false)
    }

    /**
     * Merge two constraints together — both sets of constraints must now be met.
     */
    fun and(other: Constraints): Constraints {
        if (unreachable) return this
        if (other.unreachable) return other

        return Constraints(Functional.mergeMapsUnion(this.constraints, other.constraints) { lhs, rhs ->
            fun <T : ConstrainedSet<T>> ugly(l: ConstrainedSet<T>): ConstrainedSet<T> =
                // Safety: We've asserted that the types are the same.
                @Suppress("UNCHECKED_CAST")
                l.intersect(rhs as T)

            assert(lhs.getSetIndicator() == rhs.getSetIndicator())

            ugly(lhs)
        }, false)
    }
}