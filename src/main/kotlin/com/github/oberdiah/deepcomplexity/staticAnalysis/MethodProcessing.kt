package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ConstantExpression
import com.github.oberdiah.deepcomplexity.evaluation.Expression
import com.github.oberdiah.deepcomplexity.evaluation.IncomingVariable
import com.intellij.psi.*

/**
 * Unique Variable ID
 */
typealias UVID = String

/**
 * For the moment, this is entirely within the context of a single method.
 */
class MethodContext {
    // Psi Element is where the variable is defined —
    // either PsiLocalVariable, PsiParameter, or PsiField
    val variables = mutableMapOf<PsiElement, VariableContext>()

    fun declareNewVar(element: PsiElement, expression: Expression<*>) {
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                variables[element] = VariableContext(expression)
            }

            else -> {
                TODO(
                    "As-yet unsupported PsiElement type for variable declaration"
                )
            }
        }
    }
}

/**
 * Expression is what connects the values coming in to this method with what's going out.
 * Expression is equal to whatever we've built up so far for this variable up to this point
 * in this method.
 */
class VariableContext(val expression: Expression<*>) {

}

object MethodProcessing {
    fun processMethod(method: PsiMethod) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.
        // What makes method processing nice is we don't need to think about
        // anything outside this method. Things coming in from outside are just unknowns
        // we parameterize over.

        val context = MethodContext()

        for (param in method.parameterList.parameters) {
            context.variables[param] = VariableContext(
                IncomingVariable(param)
            )
        }

        method.body?.let { body ->
//            val flow = ControlFlowFactory
//                .getControlFlow(
//                    body,
//                    AllVariablesControlFlowPolicy.getInstance(),
//                    ControlFlowOptions.create(true, true, false)
//                )

            processBody(body, context)
        }
    }

    private fun processBody(
        body: PsiCodeBlock,
        context: MethodContext
    ) {
        for (line in body.children) {
            processPsiElement(line, context)
        }
    }

    private fun processPsiElement(
        line: PsiElement,
        context: MethodContext
    ) {
        when (line) {
            is PsiExpressionStatement -> {
                processPsiElement(line.expression, context)
            }

            is PsiDeclarationStatement -> {
                // Handled similar to assignment expression
                line.declaredElements.forEach { element ->
                    if (element is PsiLocalVariable) {
                        val rExpression = element.initializer
                        if (rExpression != null) {
                            context.declareNewVar(
                                element,
                                buildExpressionFromPsi(rExpression, context)
                            )
                        } else {
                            TODO(
                                "Eventually we'll replace this with either null or 0, " +
                                        "for now it'll remain blank."
                            )
                        }
                    }

                    throw IllegalArgumentException(
                        "As-yet unsupported PsiElement type for variable declaration"
                    )
                }
            }

            is PsiAssignmentExpression -> {
                line.rExpression?.let { rExpression ->
                    context.declareNewVar(
                        line.lExpression,
                        buildExpressionFromPsi(rExpression, context)
                    )
                }
            }

            is PsiMethodCallExpression -> {
                // We're not thinking about methods yet.
            }

            is PsiIfStatement -> {
                // This is a conditional statement
            }

            is PsiReturnStatement -> {

            }

        }
    }

    /**
     * Builds up the expression tree.
     */
    fun buildExpressionFromPsi(psi: PsiExpression, context: MethodContext): Expression<*> {
        when (psi) {
            is PsiLiteralExpression -> {
                val value = psi.value ?: TODO("Not implemented yet")
                return ConstantExpression.fromAny(value)
            }
        }
        TODO("As-yet unsupported PsiExpression type")
    }
}