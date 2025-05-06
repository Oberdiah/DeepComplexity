package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.github.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.github.oberdiah.deepcomplexity.evaluation.BooleanSetIndicator
import com.github.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.github.oberdiah.deepcomplexity.solver.ConstraintSolver

fun <T : Number> BundleSet<T>.arithmeticOperation(other: BundleSet<T>, operation: BinaryNumberOp): BundleSet<T> =
    this.performBinaryOperation(other) { a, b ->
        a.into().arithmeticOperation(b.into(), operation)
    }

fun BundleSet<Boolean>.booleanOperation(other: BundleSet<Boolean>, operation: BooleanOp): BundleSet<Boolean> =
    this.performBinaryOperation(other) { a, b ->
        a.into().booleanOperation(b.into(), operation)
    }

fun <T : Number> BundleSet<T>.isOne(): Boolean = this.bundles.all {
    it.bundle.into().isOne()
}

fun <T : Number> BundleSet<T>.comparisonOperation(other: BundleSet<T>, comparisonOp: ComparisonOp): BundleSet<Boolean> =
    this.binaryMap(BooleanSetIndicator, other) { a, b ->
        a.into().comparisonOperation(b.into(), comparisonOp)
    }

fun <T : Number> BundleSet<T>.negate(): BundleSet<T> =
    this.performUnaryOperation { a ->
        a.into().negate()
    }

fun <T : Number> BundleSet<T>.getSetSatisfying(comp: ComparisonOp, key: Context.Key): BundleSet<T> =
    this.performUnaryOperation { a ->
        a.into().getSetSatisfying(comp, key)
    }

fun <T : Number> BundleSet<T>.evaluateLoopingRange(
    evaluate: ConstraintSolver.EvaluatedCollectedTerms<T>,
    constraint: NumberSet<T>
): BundleSet<T> =
    this.performUnaryOperation { a ->
        a.into().evaluateLoopingRange(evaluate, constraint)
    }