package com.oberdiah.deepcomplexity.staticAnalysis

import com.oberdiah.deepcomplexity.context.HeapMarker

fun SetIndicator<Boolean>.into(): BooleanIndicator = this as BooleanIndicator

fun <T : Number> SetIndicator<T>.into(): NumberIndicator<T> = this as NumberIndicator<T>

fun SetIndicator<HeapMarker>.into(): ObjectIndicator = this as ObjectIndicator