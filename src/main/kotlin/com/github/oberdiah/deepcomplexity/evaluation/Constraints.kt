package com.github.oberdiah.deepcomplexity.evaluation

import ai.grazie.text.TextTemplate.variable
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.IMoldableSet
import com.github.oberdiah.deepcomplexity.utilities.Functional
import org.codehaus.groovy.ast.expr.NotExpression

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
    private val constraints: Map<Context.Key, IExpr<*>>,
    // If this is true these constraints have been proven to be unsatisfiable.
    // Currently, this is the constraint you get when you do something like `if (false) { ... }`
    private val unreachable: Boolean
) {
    companion object {
        fun constrainedBy(constraints: Map<Context.Key, IExpr<*>>): Constraints {
            return Constraints(constraints, false)
        }

        fun completelyUnconstrained(): Constraints {
            return Constraints(emptyMap(), false)
        }

        fun unreachable(): Constraints {
            return Constraints(emptyMap(), true)
        }
    }

    fun <T : IMoldableSet<T>> getConstraint(variable: VariableExpression<T>): IExpr<T>? {
        return constraints[variable.getKey().key]?.let { it.tryCastTo(variable.getSetIndicator())!! }
    }

    fun addConstraint(key: Context.Key, expr: IExpr<*>): Constraints {
        return and(constrainedBy(mapOf(key to expr)))
    }

    fun invert(): Constraints {
        if (unreachable) return completelyUnconstrained()

        // You may be wondering why aren't you dealing with the non-map values?
        // I don't believe that's necessary, e.g. if (!(x > 5)) still doesn't constrain y.
        return Constraints(constraints.mapValues { (_, v) -> InvertExpression(v) }, false)
    }

    /**
     * Merge two constraints together — both sets of constraints must now be met.
     */
    fun and(other: Constraints): Constraints {
        if (unreachable) return this
        if (other.unreachable) return other

        return Constraints(Functional.mergeMapsUnion(this.constraints, other.constraints) { lhs, rhs ->
            fun <T : IMoldableSet<T>> ugly(l: IExpr<T>): IExpr<T> =
                // Safety: We've asserted that the types are the same.
                @Suppress("UNCHECKED_CAST")
                IntersectExpression(l, rhs as IExpr<T>)

            assert(lhs.getSetIndicator() == rhs.getSetIndicator())

            ugly(lhs)
        }, false)
    }
}