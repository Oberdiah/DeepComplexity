package com.github.oberdiah.deepcomplexity.staticAnalysis.sets

import com.github.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.github.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

fun <T : Number> ISet<T>.into(): NumberSet<T> =
    this as NumberSet<T>

fun ISet<Boolean>.into(): BooleanSet =
    this as BooleanSet

fun <T : Number> Variances<T>.into(): NumberVariances<T> =
    this as NumberVariances<T>

fun Variances<Boolean>.into(): BooleanVariances =
    this as BooleanVariances

fun <T : Number> SetIndicator<T>.into(): NumberSetIndicator<T> =
    this as NumberSetIndicator<T>