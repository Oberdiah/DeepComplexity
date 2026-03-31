package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.staticAnalysis.numberSimplification.ConversionsAndPromotion

fun <T : Number> Expr<*>.summedWith(other: Expr<T>): Expr<T> = arithmeticOp(other, BinaryNumberOp.ADDITION)
fun <T : Number> Expr<*>.subtractedFrom(other: Expr<T>): Expr<T> = arithmeticOp(other, BinaryNumberOp.SUBTRACTION)

@Suppress("unused")
fun <T : Number> Expr<*>.multipliedBy(other: Expr<T>): Expr<T> = arithmeticOp(other, BinaryNumberOp.MULTIPLICATION)

@Suppress("unused")
fun <T : Number> Expr<*>.dividedBy(other: Expr<T>): Expr<T> = arithmeticOp(other, BinaryNumberOp.DIVISION)

fun <T : Number> Expr<*>.arithmeticOp(other: Expr<T>, op: BinaryNumberOp): Expr<T> = ConversionsAndPromotion.coerceAToB(
    this,
    other
).map { l, r ->
    ArithmeticExpr.new(l, r, op)
}