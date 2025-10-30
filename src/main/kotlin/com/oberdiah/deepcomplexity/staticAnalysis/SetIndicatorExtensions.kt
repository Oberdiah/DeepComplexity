package com.oberdiah.deepcomplexity.staticAnalysis

import com.oberdiah.deepcomplexity.context.HeapMarker

fun SetIndicator<Boolean>.into(): BooleanSetIndicator = this as BooleanSetIndicator

fun <T : Number> SetIndicator<T>.into(): NumberSetIndicator<T> = this as NumberSetIndicator<T>

fun SetIndicator<HeapMarker>.into(): ObjectSetIndicator = this as ObjectSetIndicator