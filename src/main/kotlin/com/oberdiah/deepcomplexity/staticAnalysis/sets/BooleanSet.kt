package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.Indicator
import com.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

enum class BooleanSet : ISet<Boolean> {
    TRUE {
        override fun size(): Long = 1L

        override fun contains(element: Boolean): Boolean {
            return element
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (!other) EITHER else this
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
            return if (other) EITHER else this
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (!other) NEITHER else this
        }
    },

    // The value in question may end up being either true or false, we're not sure.
    EITHER {
        override fun size(): Long = 2L

        override fun contains(element: Boolean): Boolean {
            return true
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return EITHER
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (other) FALSE else TRUE
        }
    },

    // The value in question is invalid; we've hit a contradiction.
    // Neither is contagious; most interactions with Neither will return Neither.
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
        return this == EITHER
    }

    override fun <Q : Any> cast(newInd: Indicator<Q>): ISet<Q>? {
        throw IllegalArgumentException("Cannot cast boolean to $newInd")
    }

    override fun comparisonOperation(other: ISet<Boolean>, operation: ComparisonOp): BooleanSet {
        val other = other.into()
        if (this == NEITHER || other == NEITHER) {
            return NEITHER
        }

        return when (operation) {
            ComparisonOp.EQUAL -> {
                when (this) {
                    TRUE -> other
                    FALSE -> other.booleanInvert()
                    EITHER -> EITHER
                }
            }

            ComparisonOp.NOT_EQUAL -> {
                when (this) {
                    TRUE -> other.booleanInvert()
                    FALSE -> other
                    EITHER -> EITHER
                }
            }

            else -> throw IllegalArgumentException("Cannot perform non-equality comparison on booleans.")
        }
    }

    override val ind = BooleanIndicator

    /**
     * Inverts the boolean meaning of the set, rather than the set itself.
     *
     * (TRUE becomes FALSE, FALSE becomes TRUE, and BOTH and NEITHER remain the same.)
     */
    fun booleanInvert(): BooleanSet {
        return when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            EITHER -> EITHER
            NEITHER -> NEITHER
        }
    }

    override fun intersect(other: ISet<Boolean>): ISet<Boolean> {
        // Set intersection
        return when (other.into()) {
            TRUE -> this.removeFromSet(false)
            FALSE -> this.removeFromSet(true)
            EITHER -> this
            NEITHER -> NEITHER
        }
    }

    override fun union(other: ISet<Boolean>): ISet<Boolean> {
        // Set union
        return when (other.into()) {
            TRUE -> this.addToSet(true)
            FALSE -> this.addToSet(false)
            EITHER -> EITHER
            NEITHER -> this
        }
    }

    fun booleanOperation(other: BooleanSet, operation: BooleanOp): BooleanSet {
        if (this == NEITHER || other == NEITHER) {
            return NEITHER
        }

        return when (operation) {
            BooleanOp.AND -> {
                when (this) {
                    TRUE -> other
                    FALSE -> FALSE
                    EITHER -> EITHER
                }
            }

            BooleanOp.OR -> {
                when (this) {
                    TRUE -> TRUE
                    FALSE -> other
                    EITHER -> EITHER
                }
            }
        }
    }

    abstract fun addToSet(other: Boolean): BooleanSet
    abstract fun removeFromSet(other: Boolean): BooleanSet
}