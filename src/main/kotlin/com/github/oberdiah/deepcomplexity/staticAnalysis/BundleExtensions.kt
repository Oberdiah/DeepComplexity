package com.github.oberdiah.deepcomplexity.staticAnalysis

fun <T : Number> Bundle<T>.into(): NumberSet<T> =
    this as NumberSet<T>

fun Bundle<Boolean>.into(): BooleanSet =
    this as BooleanSet