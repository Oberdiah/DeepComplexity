package com.oberdiah.deepcomplexity.staticAnalysis.sets

import com.oberdiah.deepcomplexity.context.HeapMarker
import com.oberdiah.deepcomplexity.staticAnalysis.NumberSetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.SetIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.variances.BooleanVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.ObjectVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances

fun <T : Number> ISet<T>.into(): NumberSet<T> =
    this as NumberSet<T>

fun ISet<Boolean>.into(): BooleanSet =
    this as BooleanSet

fun ISet<HeapMarker>.into(): ObjectSet =
    this as ObjectSet

fun <T : Number> Variances<T>.into(): NumberVariances<T> =
    this as NumberVariances<T>

fun Variances<Boolean>.into(): BooleanVariances =
    this as BooleanVariances

fun Variances<HeapMarker>.into(): ObjectVariances =
    this as ObjectVariances

fun <T : Number> SetIndicator<T>.into(): NumberSetIndicator<T> =
    this as NumberSetIndicator<T>
