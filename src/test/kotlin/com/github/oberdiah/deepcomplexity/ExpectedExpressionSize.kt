package com.oberdiah.deepcomplexity

@Retention(AnnotationRetention.RUNTIME) // Make it accessible at runtime
@Target(AnnotationTarget.FUNCTION) // Target functions only
annotation class ExpectedExpressionSize(val value: Int)
