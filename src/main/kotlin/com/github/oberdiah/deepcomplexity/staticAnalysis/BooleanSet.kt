package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.evaluation.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.SetIndicator

enum class BooleanSet : Bundle<Boolean> {
    TRUE {
        override fun contains(other: Boolean): Boolean {
            return other
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (!other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (other) NEITHER else this
        }
    },
    FALSE {
        override fun contains(other: Boolean): Boolean {
            return !other
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return if (!other) NEITHER else this
        }
    },
    BOTH {
        override fun contains(other: Boolean): Boolean {
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
        override fun contains(other: Boolean): Boolean {
            return false
        }

        override fun addToSet(other: Boolean): BooleanSet {
            return if (other) TRUE else FALSE
        }

        override fun removeFromSet(other: Boolean): BooleanSet {
            return NEITHER
        }
    };

    class BooleanVariance(private val value: BooleanSet) : VarianceBundle<Boolean> {
        fun invert(): VarianceBundle<Boolean> = BooleanVariance(value.invert())
        override fun getIndicator(): SetIndicator<Boolean> = value.getIndicator()
        override fun union(other: VarianceBundle<Boolean>): VarianceBundle<Boolean> = when (other) {
            is BooleanVariance -> BooleanVariance(value.union(other.value).into())
            else -> throw IllegalArgumentException("Cannot union $other to BooleanVariance")
        }

        override fun <Q : Any> cast(newInd: SetIndicator<Q>): VarianceBundle<Q>? =
            throw IllegalArgumentException("Cannot cast boolean to $newInd")

        override fun collapse(): Bundle<Boolean> = value

        fun booleanOperation(other: BooleanVariance, operation: BooleanOp): BooleanVariance {
            return BooleanVariance(value.booleanOperation(other.value, operation))
        }
    }

    companion object {
        fun fromBoolean(value: Boolean): BooleanSet {
            return if (value) TRUE else FALSE
        }
    }

    override fun withVariance(key: Context.Key): VarianceBundle<Boolean> = BooleanVariance(this)

    override fun isEmpty(): Boolean {
        return this == NEITHER
    }

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>? {
        throw IllegalArgumentException("Cannot cast boolean to $newInd")
    }

    override fun getIndicator(): SetIndicator<Boolean> {
        return BooleanSetIndicator
    }

    override fun invert(): BooleanSet {
        // This is a set invert, not a boolean invert
        return when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            BOTH -> NEITHER
            NEITHER -> BOTH
        }
    }

    override fun intersect(other: Bundle<Boolean>): Bundle<Boolean> {
        // Set intersection
        return when (other.into()) {
            TRUE -> this.removeFromSet(false)
            FALSE -> this.removeFromSet(true)
            BOTH -> this
            NEITHER -> NEITHER
        }
    }

    override fun union(other: Bundle<Boolean>): Bundle<Boolean> {
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