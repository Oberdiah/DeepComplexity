package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

fun <T : Number> Bundle<T>.into(): NumberSet<T> =
    this as NumberSet<T>

fun Bundle<Boolean>.into(): BooleanSet =
    this as BooleanSet

fun <T : Number> Variances<T>.into(): NumberVariances<T> =
    this as NumberVariances<T>

fun Variances<Boolean>.into(): BooleanSet.BooleanVariances =
    this as BooleanSet.BooleanVariances