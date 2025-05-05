package com.github.oberdiah.deepcomplexity.evaluation

import com.github.oberdiah.deepcomplexity.staticAnalysis.BundleSet
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
    private val constraints: Map<Context.Key, BundleSet<*>>,
    // If this is true these constraints have been proven to be unsatisfiable.
    // Currently, this is the constraint you get when you do something like `if (false) { ... }`
    private val unreachable: Boolean
) {
    companion object {
        fun constrainedBy(constraints: Map<Context.Key, BundleSet<*>>): Constraints {
            return Constraints(constraints, false)
        }

        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap(), false)
        }

        fun unreachable(): Constraints {
            return Constraints(emptyMap(), true)
        }
    }

    fun isUnreachable(): Boolean {
        return unreachable
    }

    fun <T : Any> getConstraint(variable: VariableExpression<T>): BundleSet<T>? {
        return constraints[variable.getKey().key]?.cast(variable.getSetIndicator())
    }

    fun <T : Any> getConstraint(ind: SetIndicator<T>, key: Context.Key): BundleSet<T>? {
        return constraints[key]?.cast(ind)
    }

    fun addConstraint(key: Context.Key, expr: BundleSet<*>): Constraints {
        return and(constrainedBy(mapOf(key to expr)))
    }

    fun invert(): Constraints {
        if (unreachable) return completelyUnconstrained()

        // You may be wondering why you aren't dealing with the non-map values?
        // I don't believe that's necessary, e.g., if (!(x > 5)) still doesn't constrain y.
        return Constraints(constraints.mapValues { (_, v) -> v.invert() }, false)
    }

    /**
     * Merge two constraints — both sets of constraints must now be met.
     */
    fun and(other: Constraints): Constraints {
        if (unreachable) return this
        if (other.unreachable) return other

        // todo: Actually check here and make sure the resulting constraints are still doable
        //  (i.e. none are the empty set)

        return Constraints(Functional.mergeMapsUnion(this.constraints, other.constraints) { lhs, rhs ->
            fun <T : Any> ugly(l: BundleSet<T>): BundleSet<T> =
                // Safety: We've asserted that the types are the same.
                @Suppress("UNCHECKED_CAST")
                l.intersect(rhs as BundleSet<T>)

            assert(lhs.ind == rhs.ind)

            ugly(lhs)
        }, false)
    }
}