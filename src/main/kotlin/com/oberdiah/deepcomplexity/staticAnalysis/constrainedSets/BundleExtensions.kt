package com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets

import com.oberdiah.deepcomplexity.context.EvaluationKey
import com.oberdiah.deepcomplexity.evaluation.BinaryNumberOp
import com.oberdiah.deepcomplexity.evaluation.BooleanOp
import com.oberdiah.deepcomplexity.evaluation.ComparisonOp
import com.oberdiah.deepcomplexity.staticAnalysis.BooleanIndicator
import com.oberdiah.deepcomplexity.staticAnalysis.constrainedSets.Bundle.ConstrainedVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.NumberVariances
import com.oberdiah.deepcomplexity.staticAnalysis.variances.Variances
import com.oberdiah.deepcomplexity.utilities.into

/**
 * When a binary operation demolishes variance tracking (e.g. modulo, multiply of two tracked values),
 * we can rescue the result by storing it as a constraint on the expression key. This allows us to
 * continue tracking things to a limited degree even when the operation destroyed our typical variance tracking.
 *
 * Only rescues if we were tracking something beforehand (otherwise we'd just be tracking a constant for
 * no reason), and only for numeric variances.
 */
fun <T : Any> rescueVariances(
    exprKey: EvaluationKey<*>,
    lhs: Variances<*>,
    rhs: Variances<*>,
    result: Variances<T>,
    constraints: Constraints,
): ConstrainedVariances<T> {
    val wereTrackingSomething = (lhs.varsTracking() + rhs.varsTracking()).isNotEmpty()
    if (result.varsTracking().isEmpty() && wereTrackingSomething && result is NumberVariances<*>) {
        return ConstrainedVariances.fromKeyAndSet<T>(exprKey, result.collapse(constraints))
            .andAlsoWithConstraints(constraints)
    }
    return ConstrainedVariances.new(result, constraints)
}

fun <T : Number> Bundle<T>.arithmeticOperation(
    other: Bundle<T>,
    operation: BinaryNumberOp,
    exprKey: EvaluationKey<*>
): Bundle<T> =
    this.binaryMapSameType(other) { a, b, constraints ->
        val result = a.into().arithmeticOperation(b.into(), operation, constraints)
        rescueVariances(exprKey, a, b, result, constraints)
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
    exprKey: EvaluationKey<*>
): Bundle<Boolean> =
    this.binaryMapSameType(other) { a, b, constraints ->
        val result = a.into().booleanOperation(b.into(), operation)
        rescueVariances(exprKey, a, b, result, constraints)
    }

fun Bundle<Boolean>.booleanInvert() = unaryMapSameType { variances, _ ->
    variances.into().booleanInvert()
}

fun <T : Any> Bundle<T>.comparisonOperation(
    other: Bundle<T>,
    comparisonOp: ComparisonOp,
    exprKey: EvaluationKey<*>
): Bundle<Boolean> {
    return this.binaryMap(BooleanIndicator, other) { a, b, constraints ->
        val result = a.comparisonOperation(b, comparisonOp, constraints)
        rescueVariances(exprKey, a, b, result, constraints)
    }
}

fun <T : Number> Bundle<T>.negate(): Bundle<T> =
    this.unaryMapSameType { variances, _ ->
        variances.into().negate()
    }