package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

enum class BooleanSet : MoldableSet {
    TRUE {
        override fun contains(other: Boolean): Boolean {
            return other
        }

        override fun with(other: Boolean): BooleanSet {
            return if (other) TRUE else BOTH
        }
    },
    FALSE {
        override fun contains(other: Boolean): Boolean {
            return !other
        }

        override fun with(other: Boolean): BooleanSet {
            return if (other) BOTH else FALSE
        }
    },
    BOTH {
        override fun contains(other: Boolean): Boolean {
            return true
        }

        override fun with(other: Boolean): BooleanSet {
            return BOTH
        }
    };

    companion object {
        fun fromBoolean(value: Boolean): BooleanSet {
            return if (value) TRUE else FALSE
        }
    }

    abstract fun with(other: Boolean): BooleanSet
    abstract fun contains(other: Boolean): Boolean

    override fun union(other: MoldableSet): MoldableSet {
        return when (other) {
            TRUE -> if (this == FALSE) BOTH else TRUE
            FALSE -> if (this == TRUE) BOTH else FALSE
            BOTH -> BOTH
            else -> throw IllegalArgumentException("Cannot union $this with $other")
        }
    }

    override fun getClass(): KClass<*> {
        return Boolean::class
    }
}