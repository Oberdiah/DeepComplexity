package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

enum class BooleanSet : IMoldableSet {
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

    companion object {
        fun fromBoolean(value: Boolean): BooleanSet {
            return if (value) TRUE else FALSE
        }
    }

    override fun invert(): IMoldableSet {
        // This is a set invert, not a boolean invert
        return when (this) {
            TRUE -> FALSE
            FALSE -> TRUE
            BOTH -> NEITHER
            NEITHER -> BOTH
        }
    }

    override fun intersect(other: IMoldableSet): IMoldableSet {
        // Set intersection
        return when (other) {
            TRUE -> this.removeFromSet(false)
            FALSE -> this.removeFromSet(true)
            BOTH -> this
            NEITHER -> NEITHER
            else -> throw IllegalArgumentException("Cannot intersect with $other")
        }
    }

    override fun union(other: IMoldableSet): IMoldableSet {
        // Set union
        return when (other) {
            TRUE -> this.addToSet(true)
            FALSE -> this.addToSet(false)
            BOTH -> BOTH
            NEITHER -> this
            else -> throw IllegalArgumentException("Cannot union with $other")
        }
    }

    abstract fun addToSet(other: Boolean): BooleanSet
    abstract fun removeFromSet(other: Boolean): BooleanSet
    abstract fun contains(other: Boolean): Boolean

    override fun getClass(): KClass<*> {
        return Boolean::class
    }
}