package com.github.oberdiah.deepcomplexity.staticAnalysis.bundles

import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

fun <T : Number> Bundle<T>.into(): NumberBundle<T> =
    this as NumberBundle<T>

fun Bundle<Boolean>.into(): BooleanBundle =
    this as BooleanBundle

fun <T : Number> Variances<T>.into(): NumberVariances<T> =
    this as NumberVariances<T>

fun Variances<Boolean>.into(): BooleanBundle.BooleanVariances =
    this as BooleanBundle.BooleanVariances