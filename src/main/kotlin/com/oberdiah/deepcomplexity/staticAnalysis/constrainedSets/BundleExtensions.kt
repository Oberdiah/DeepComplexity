package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.utilities.into

fun <T : Number> Bundle<T>.arithmeticOperation(
    other: Bundle<T>,
    operation: BinaryNumberOp,
    exprKey: EvaluationKey<T>? = null
): Bundle<T> =
    this.binaryMapWithRescue(ind, other, exprKey) { a, b, constraints ->
        a.into().arithmeticOperation(b.into(), operation, constraints)
    }

fun <T : Any> Bundle<T>.generateConstraintsFrom(
    other: Bundle<T>,
    operation: ComparisonOp,
): ConstraintsOrPile {
    if (this.isEmpty() || other.isEmpty()) {
        return ConstraintsOrPile.unreachable()
    }

    val newConstraintsGenerated = this.binaryMapToList(other) { a, b, constraints ->
        a.generateConstraintsFrom(b, operation, constraints)
    }.toSet()

    return ConstraintsOrPile(newConstraintsGenerated)
}

fun Bundle<Boolean>.booleanOperation(
    other: Bundle<Boolean>,
    operation: BooleanOp,
    exprKey: EvaluationKey<Boolean>? = null
): Bundle<Boolean> =
    this.binaryMapWithRescue(ind, other, exprKey) { a, b, constraints ->
        a.into().booleanOperation(b.into(), operation, constraints)
    }

fun Bundle<Boolean>.booleanInvert() = unaryMapSameType { variances, _ ->
    variances.into().booleanInvert()
}

fun <T : Any> Bundle<T>.comparisonOperation(
    other: Bundle<T>,
    comparisonOp: ComparisonOp,
    exprKey: EvaluationKey<Boolean>? = null
): Bundle<Boolean> {
    return this.binaryMapWithRescue(BooleanIndicator, other, exprKey) { a, b, constraints ->
        a.comparisonOperation(b, comparisonOp, constraints)
    }
}

fun <T : Number> Bundle<T>.negate(): Bundle<T> =
    this.unaryMapSameType { variances, _ ->
        variances.into().negate()
    }