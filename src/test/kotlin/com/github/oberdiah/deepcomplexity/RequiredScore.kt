package com.oberdiah.deepcomplexity

@Retention(AnnotationRetention.RUNTIME) // Make it accessible at runtime
@Target(AnnotationTarget.FUNCTION) // Target functions only
annotation class RequiredScore(val value: Double)
