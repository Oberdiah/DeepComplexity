package com.oberdiah.deepcomplexity

@Retention(AnnotationRetention.RUNTIME) // Make it accessible at runtime
@Target(AnnotationTarget.FUNCTION) // Target functions only
/**
 * Doesn't do anything, just adds a little checkmark in the summary page to let us know it's
 * good enough.
 */
annotation class GoodEnough(val value: GoodEnoughReason) {
    enum class GoodEnoughReason {
        /**
         * When there are gaps in the result purely due to not taking into account
         * holes caused by integer multiplication.
         */
        GAPS_FROM_MULTIPLICATION,

        /**
         * When there are gaps in the result purely due to not tracking
         * higher powers of x — e.g. x^2
         */
        GAPS_FROM_POWERS,

        /**
         * When there are gaps in the result due to not taking into account the behaviour of modulus.
         */
        REQUIRES_KNOWLEDGE_OF_MODULUS
    }
}