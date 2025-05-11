package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.evaluation.Constraints
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.Context
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

class GenericSet<T : Any>(val values: Set<T>) : Bundle<T> {
    class GenericVariances<T : Any>(private val value: GenericSet<T>) : Variances<T> {
        override val ind: SetIndicator<T>
            get() = TODO("Not yet implemented")

        override fun toDebugString(constraints: Constraints): String {
            TODO("Not yet implemented")
        }

        override fun collapse(constraints: Constraints): Bundle<T> = value

        override fun <Q : Any> cast(newInd: SetIndicator<Q>): Variances<Q>? {
            TODO("Not yet implemented")
        }
    }

    override fun toConstVariance(): Variances<T> {
        TODO("Not yet implemented")
    }

    override fun withVariance(key: Context.Key): Variances<T> {
        TODO("Not yet implemented")
    }

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override val ind: SetIndicator<T>
        get() = TODO("Not yet implemented")

    override fun invert(): Bundle<T> {
        TODO("Not yet implemented")
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>? {
        TODO("Not yet implemented")
    }

    override fun contains(element: T): Boolean {
        TODO("Not yet implemented")
    }

    override fun intersect(other: Bundle<T>): Bundle<T> {
        TODO("Not yet implemented")
    }

    override fun union(other: Bundle<T>): Bundle<T> {
        TODO("Not yet implemented")
    }
}