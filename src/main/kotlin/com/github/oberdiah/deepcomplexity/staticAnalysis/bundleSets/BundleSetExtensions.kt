package com.github.oberdiah.deepcomplexity.staticAnalysis.bundleSets

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver
import com.github.oberdiah.deepcomplexity.staticAnalysis.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.NumberBundle
import com.github.oberdiah.deepcomplexity.staticAnalysis.bundles.into

fun <T : Number> BundleSet<T>.arithmeticOperation(other: BundleSet<T>, operation: BinaryNumberOp): BundleSet<T> =
    this.performBinaryOperation(other) { a, b, constraints ->
        a.into().arithmeticOperation(b.into(), operation, constraints)
    }

fun BundleSet<Boolean>.booleanOperation(other: BundleSet<Boolean>, operation: BooleanOp): BundleSet<Boolean> =
    this.performBinaryOperation(other) { a, b, constraints ->
        a.into().booleanOperation(b.into(), operation)
    }

fun BundleSet<Boolean>.invert() = performUnaryOperation { it.into().invert() }

fun <T : Number> BundleSet<T>.isOne(): Boolean = this.bundles.all {
    it.bundle.into().isOne(it.constraints)
}

fun <T : Number> BundleSet<T>.comparisonOperation(other: BundleSet<T>, comparisonOp: ComparisonOp): BundleSet<Boolean> =
    this.binaryMap(BooleanSetIndicator, other) { a, b, constraints ->
        a.into().comparisonOperation(b.into(), comparisonOp, constraints)
    }

fun <T : Number> BundleSet<T>.negate(): BundleSet<T> =
    this.performUnaryOperation { a ->
        a.into().negate()
    }

fun <T : Number> BundleSet<T>.evaluateLoopingRange(
    evaluate: ConstraintSolver.EvaluatedCollectedTerms<T>,
    constraint: NumberBundle<T>
): BundleSet<T> =
    this.performUnaryOperation { a ->
        a.into().evaluateLoopingRange(evaluate, constraint)
    }