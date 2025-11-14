package com.oberdiah.deepcomplexity.staticAnalysis

import com.oberdiah.deepcomplexity.context.HeapMarker

fun Indicator<Boolean>.into(): BooleanIndicator = this as BooleanIndicator

fun <T : Number> Indicator<T>.into(): NumberIndicator<T> = this as NumberIndicator<T>

fun Indicator<HeapMarker>.into(): ObjectIndicator = this as ObjectIndicator