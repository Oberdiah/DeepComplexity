package com.github.oberdiah.deepcomplexity.staticAnalysis

import kotlin.reflect.KClass

enum class BooleanSet : MoldableSet<Boolean> {
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

    abstract fun with(other: Boolean): BooleanSet

    override fun getClass(): KClass<*> {
        return Boolean::class
    }
}