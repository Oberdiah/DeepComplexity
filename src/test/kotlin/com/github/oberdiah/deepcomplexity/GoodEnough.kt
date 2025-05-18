package com.github.oberdiah.deepcomplexity

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
         * To resolve these situations further, we would need to identify when two black-box functions
         * are the same and then assign them the same ID.
         *
         * Currently, we're treating modulo (%) as a black-box function. There is an argument to be made
         * that we should be able to track it better, and if we do do that, the functions marked with this
         * annotation will want to be updated to continue using black-box functions.
         */
        REQUIRES_IDENTIFYING_IDENTICAL_EXPRESSIONS,
    }
}