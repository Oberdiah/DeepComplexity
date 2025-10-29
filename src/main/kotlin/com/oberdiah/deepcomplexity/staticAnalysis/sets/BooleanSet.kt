package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

enum class BooleanSet : ISet<Boolean> {
    TRUE {
        override fun size(): Long = 1L

        override fun contains(element: Boolean): Boolean {
            return element
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (!other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (other) NEITHER else this
        }
    },
    FALSE {
        override fun size(): Long = 1L

        override fun contains(element: Boolean): Boolean {
            return !element
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (!other) NEITHER else this
        }
    },
    BOTH {
        override fun size(): Long = 2L

        override fun contains(element: Boolean): Boolean {
            return true
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return BOTH
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (other) FALSE else TRUE
        }
    },
    NEITHER {
        override fun size(): Long = 0L

        override fun contains(element: Boolean): Boolean {
            return false
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (other) TRUE else FALSE
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return NEITHER
        }
    };

    companion object {
        fun fromBoolean(value: Boolean): BooleanSet {
            return if (value) TRUE else FALSE
        }
    }

    override fun toConstVariance(): Variances<Boolean> {
        return BooleanVariances(this)
    }

    override fun isEmpty(): Boolean {
        return this == NEITHER
    }

    override fun isFull(): Boolean {
        return this == BOTH
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): ISet<Q>? {
        throw IllegalArgumentException("Cannot cast boolean to $newInd")
    }

    override fun comparisonOperation(other: ISet<Boolean>, operation: ComparisonOp): BooleanSet {
        TODO("Not yet implemented")
    }

    override val ind = BooleanSetIndicator

    override fun invert(): BooleanSet {
        // This is a set invert, not a boolean invert
        return when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            BOTH -> NEITHER
            NEITHER -> BOTH
        }
    }

    /**
     * Inverts the boolean meaning of the set, rather than the set itself.
     *
     * (TRUE becomes FALSE, FALSE becomes TRUE, and BOTH and NEITHER remain the same.)
     */
    fun booleanInvert(): BooleanSet {
        return when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            BOTH -> BOTH
            NEITHER -> NEITHER
        }
    }

    override fun intersect(other: ISet<Boolean>): ISet<Boolean> {
        // Set intersection
        return when (other.into()) {
            TRUE -> this.removeFromSet(false)
            FALSE -> this.removeFromSet(true)
            BOTH -> this
            NEITHER -> NEITHER
        }
    }

    override fun union(other: ISet<Boolean>): ISet<Boolean> {
        // Set union
        return when (other.into()) {
            TRUE -> this.addToSet(true)
            FALSE -> this.addToSet(false)
            BOTH -> BOTH
            NEITHER -> this
        }
    }

    fun booleanOperation(other: BooleanSet, operation: BooleanOp): BooleanSet {
        return when (operation) {
            BooleanOp.AND -> {
                when (this) {
                    TRUE -> other
                    FALSE -> if (other == NEITHER) NEITHER else FALSE
                    BOTH -> if (other == NEITHER) NEITHER else BOTH
                    NEITHER -> NEITHER
                }
            }

            BooleanOp.OR -> {
                when (this) {
                    TRUE -> if (other == NEITHER) NEITHER else TRUE
                    FALSE -> other
                    BOTH -> if (other == NEITHER) NEITHER else BOTH
                    NEITHER -> NEITHER
                }
            }
        }
    }

    abstract fun addToSet(other: Boolean): BooleanSet
    abstract fun removeFromSet(other: Boolean): BooleanSet
}