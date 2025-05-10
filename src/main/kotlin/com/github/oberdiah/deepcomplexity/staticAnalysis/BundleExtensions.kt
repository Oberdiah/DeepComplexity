package com.github.oberdiah.deepcomplexity.staticAnalysis

fun <T : Number> Bundle<T>.into(): NumberSet<T> =
    this as NumberSet<T>

fun Bundle<Boolean>.into(): BooleanSet =
    this as BooleanSet

fun <T : Number> VarianceBundle<T>.into(): NumberVariance<T> =
    this as NumberVariance<T>

fun VarianceBundle<Boolean>.into(): BooleanSet.BooleanVariance =
    this as BooleanSet.BooleanVariance