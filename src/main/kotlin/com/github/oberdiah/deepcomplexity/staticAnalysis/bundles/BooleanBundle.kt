package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

enum class BooleanBundle : Bundle<Boolean> {
    TRUE {
        override fun contains(other: Boolean): Boolean {
            return other
        }

        override fun addToSet(other: Boolean): BooleanBundle {
            return if (!other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanBundle {
            return if (other) NEITHER else this
        }
    },
    FALSE {
        override fun contains(other: Boolean): Boolean {
            return !other
        }

        override fun addToSet(other: Boolean): BooleanBundle {
            return if (other) BOTH else this
        }

        override fun removeFromSet(other: Boolean): BooleanBundle {
            return if (!other) NEITHER else this
        }
    },
    BOTH {
        override fun contains(other: Boolean): Boolean {
            return true
        }

        override fun addToSet(other: Boolean): BooleanBundle {
            return BOTH
        }

        override fun removeFromSet(other: Boolean): BooleanBundle {
            return if (other) FALSE else TRUE
        }
    },
    NEITHER {
        override fun contains(other: Boolean): Boolean {
            return false
        }

        override fun addToSet(other: Boolean): BooleanBundle {
            return if (other) TRUE else FALSE
        }

        override fun removeFromSet(other: Boolean): BooleanBundle {
            return NEITHER
        }
    };

    companion object {
        fun fromBoolean(value: Boolean): BooleanBundle {
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

    override fun <Q : Any> cast(newInd: SetIndicator<Q>): Bundle<Q>? {
        throw IllegalArgumentException("Cannot cast boolean to $newInd")
    }

    override val ind = BooleanSetIndicator

    override fun invert(): BooleanBundle {
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

    fun booleanOperation(other: BooleanBundle, operation: BooleanOp): BooleanBundle {
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

    abstract fun addToSet(other: Boolean): BooleanBundle
    abstract fun removeFromSet(other: Boolean): BooleanBundle
}