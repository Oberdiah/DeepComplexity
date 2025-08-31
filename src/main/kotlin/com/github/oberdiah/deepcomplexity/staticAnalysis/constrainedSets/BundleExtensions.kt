package com.github.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.evaluation.Context
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.NumberSet
import com.github.oberdiah.deepcomplexity.staticAnalysis.sets.into

fun <T : Number> Bundle<T>.arithmeticOperation(
    other: Bundle<T>,
    operation: BinaryNumberOp,
    exprKey: Context.Key
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
    exprKey: Context.Key
): Bundle<Boolean> =
    this.performBinaryOperation(other, exprKey) { a, b, constraints ->
        a.into().booleanOperation(b.into(), operation)
    }

fun Bundle<Boolean>.invert() = performUnaryOperation {
    it.into().invert()
}

fun <T : Number> Bundle<T>.isOne(): Boolean = this.variances.all {
    it.variances.into().isOne(it.constraints)
}

fun <T : Any> Bundle<T>.comparisonOperation(
    other: Bundle<T>,
    comparisonOp: ComparisonOp,
    exprKey: Context.Key
): Bundle<Boolean> {
    return this.binaryMapToVariances(BooleanSetIndicator, other, exprKey) { a, b, constraints ->
        a.comparisonOperation(b, comparisonOp, constraints)
    }
}

fun <T : Number> Bundle<T>.numberComparisonOperation(
    other: Bundle<T>,
    comparisonOp: ComparisonOp,
    exprKey: Context.Key
): Bundle<Boolean> {
    return this.binaryMapToVariances(BooleanSetIndicator, other, exprKey) { a, b, constraints ->
        a.into().comparisonOperation(b.into(), comparisonOp, constraints)
    }
}

fun <T : Number> Bundle<T>.negate(): Bundle<T> =
    this.performUnaryOperation { a ->
        a.into().negate()
    }

fun <T : Number> Bundle<T>.evaluateLoopingRange(
    evaluate: ConstraintSolver.CollectedTerms<T>,
    constraint: NumberSet<T>
): Bundle<T> =
    this.performUnaryOperation { a ->
        a.into().evaluateLoopingRange(evaluate, constraint)
    }