package com.github.oberdiah.deepcomplexity.settings

object Settings {
    enum class OverflowBehaviour {
        /**
         * Ranges are processed assuming we never overflow.
         */
        CLAMP,

        /**
         * Ranges are processed assuming we may overflow.
         */
        ALLOW,
    }

    var overflowBehaviour: OverflowBehaviour = OverflowBehaviour.CLAMP
    var overflowWarns: Boolean = false

}