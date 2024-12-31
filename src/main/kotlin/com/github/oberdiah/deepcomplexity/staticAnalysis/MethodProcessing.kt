package com.github.oberdiah.deepcomplexity.staticAnalysis

import com.github.oberdiah.deepcomplexity.evaluation.ConstantExpression
import com.github.oberdiah.deepcomplexity.evaluation.Expression
import com.github.oberdiah.deepcomplexity.evaluation.UnresolvedVariable
import com.intellij.psi.*

/**
 * Unique Variable ID
 */
typealias UVID = String

/**
 * For the moment, this is entirely within the context of a single method.
 *
 * The context represents the state of the variables at this point in time.
 * If you're passed a Context, you can safely assume all variables in there
 * have the states specified
 */
class Context {
    // Psi Element is where the variable is defined —
    // either PsiLocalVariable, PsiParameter, or PsiField
    private val variables = mutableMapOf<PsiElement, VariableContext>()

    override fun toString(): String {
        return variables.toString()
    }

    fun getVar(element: PsiElement): VariableContext {
        return variables[element] ?: VariableContext(UnresolvedVariable(element))
    }

    fun assignVar(element: PsiElement, expression: Expression<*>) {
        when (element) {
            is PsiLocalVariable, is PsiParameter, is PsiField -> {
                variables[element] = VariableContext(expression)
            }

            is PsiReferenceExpression -> {
                // If we're assigning to a variable that's already been declared, overwrite it
                // with the new value. Otherwise, create a new variable.
                val variable = element.resolve() ?: TODO(
                    "Variable couldn't be resolved (${element.text})"
                )

                variables[variable] = VariableContext(expression)
            }

            else -> {
                TODO(
                    "As-yet unsupported PsiElement type (${element::class}) for variable declaration"
                )
            }
        }
    }

    /**
     * Expression is a converter from the values coming in with what's going out.
     * Expression is equal to whatever we've built up so far for
     * this variable up to this point.
     */
    class VariableContext(val expression: Expression<*>) {
        override fun toString(): String {
            return expression.toString()
        }
    }
}

object MethodProcessing {
    fun processMethod(method: PsiMethod) {
        // The key about this parsing operation is we want to be able to do it in O(n) time
        // where n is the size of the project.
        // What makes method processing nice is we don't need to think about
        // anything outside this method. Things coming in from outside are just unknowns
        // we parameterize over.

        val context = Context()

        method.body?.let { body ->
//            val flow = ControlFlowFactory
//                .getControlFlow(
//                    body,
//                    AllVariablesControlFlowPolicy.getInstance(),
//                    ControlFlowOptions.create(true, true, false)
//                )

            processBody(body, context)
        }

        println(context)
    }

    private fun processBody(
        body: PsiCodeBlock,
        context: Context
    ) {
        for (line in body.children) {
            processPsiElement(line, context)
        }
    }

    private fun processPsiElement(
        line: PsiElement,
        context: Context
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
                            context.assignVar(
                                element,
                                buildExpressionFromPsi(rExpression, context)
                            )
                        } else {
                            TODO(
                                "Eventually we'll replace this with either null or 0, " +
                                        "for now it'll remain blank."
                            )
                        }
                    } else {
                        throw IllegalArgumentException(
                            "As-yet unsupported PsiElement type for variable declaration: $element"
                        )
                    }
                }
            }

            is PsiAssignmentExpression -> {
                line.rExpression?.let { rExpression ->
                    context.assignVar(
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
     *
     * Nothing in here should be declaring variables.
     */
    private fun buildExpressionFromPsi(psi: PsiExpression, context: Context): Expression<*> {
        when (psi) {
            is PsiLiteralExpression -> {
                val value = psi.value ?: TODO("Not implemented yet")
                return ConstantExpression.fromAny(value)
            }

            is PsiReferenceExpression -> {
                val variable = psi.resolve()
                if (variable != null) {
                    return context.getVar(variable).expression
                } else {
                    TODO(
                        "Variable not found ${psi.text}"
                    )
                }
            }
        }
        TODO("As-yet unsupported PsiExpression type ${psi::class}")
    }
}