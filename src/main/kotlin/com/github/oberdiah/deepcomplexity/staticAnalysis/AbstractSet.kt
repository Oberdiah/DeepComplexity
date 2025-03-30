package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import kotlin.reflect.KClass

/**
 * This is the base class for all sets.
 *
 * This class handles the complexities of constraints.
 *
 * A PureSet in this context is any set that could be encountered in branchless code.
 *
 * It is a lot easier to reason about.
 */
abstract class AbstractSet<Self : AbstractSet<Self, PureSet>, PureSet>(
    private val ind: SetIndicator<Self>,
    elements: List<PureSet> // Temporary until we get things resolved in TypedNumberSet, then this can become List<ConstrainedSet<PureSet>>
) : IMoldableSet<Self> {
    val elements: List<ConstrainedSet<PureSet>> =
        elements.map { ConstrainedSet(Constraints.completelyUnconstrained(), it) }

    data class ConstrainedSet<PureSet>(val constraints: Constraints, val set: PureSet)

    override fun getSetIndicator(): SetIndicator<Self> = ind
    protected val clazz: KClass<*> = ind.clazz

    override fun hashCode(): Int = elements.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AbstractSet<*, *>) return false
        if (clazz != other.clazz) return false
        if (elements != other.elements) return false

        return true
    }
}