package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.evaluation.Key
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.sets.into

fun <T : Number> Bundle<T>.arithmeticOperation(
    other: Bundle<T>,
    operation: BinaryNumberOp,
    exprKey: Key
): Bundle<T> =
    this.performBinaryOperation(other, exprKey) { a, b, constraints ->
        a.into().arithmeticOperation(b.into(), operation, constraints)
    }

fun <T : Any> Bundle<T>.generateConstraintsFrom(
    other: Bundle<T>,
    operation: ComparisonOp,
): Set<Constraints> {
    return this.binaryMap(other) { a, b, constraints ->
        a.generateConstraintsFrom(b, operation, constraints)
    }.toSet()
}

fun Bundle<Boolean>.booleanOperation(
    other: Bundle<Boolean>,
    operation: BooleanOp,
    exprKey: Key
): Bundle<Boolean> =
    this.performBinaryOperation(other, exprKey) { a, b, constraints ->
        a.into().booleanOperation(b.into(), operation)
    }

fun Bundle<Boolean>.booleanInvert() = performUnaryOperation {
    it.into().booleanInvert()
}

fun <T : Number> Bundle<T>.isOne(): Boolean = this.variances.all {
    it.variances.into().isOne(it.constraints)
}

fun <T : Any> Bundle<T>.comparisonOperation(
    other: Bundle<T>,
    comparisonOp: ComparisonOp,
    exprKey: Key
): Bundle<Boolean> {
    return this.binaryMapToVariances(BooleanIndicator, other, exprKey) { a, b, constraints ->
        a.comparisonOperation(b, comparisonOp, constraints)
    }
}

fun <T : Number> Bundle<T>.negate(): Bundle<T> =
    this.performUnaryOperation { a ->
        a.into().negate()
    }