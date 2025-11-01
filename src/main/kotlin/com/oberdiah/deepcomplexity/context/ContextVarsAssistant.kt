package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.context.Context.KeyBackreference
import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.VariableExpr
import com.oberdiah.deepcomplexity.staticAnalysis.ObjectSetIndicator

object ContextVarsAssistant {
    fun getVar(vars: Vars, key: UnknownKey, makeBackreference: (UnknownKey) -> KeyBackreference): Expr<*> {
        // If we have it, return it.
        vars[key]?.let { return it.getDynExpr() }

        // If we don't, before we create a new variable expression, we need to check in case there's a placeholder
        if (key is QualifiedFieldKey) {
            val placeholderQualifierKey =
                makeBackreference(PlaceholderKey(key.qualifier.ind as ObjectSetIndicator))

            val replacementQualified = VariableExpr.new(makeBackreference(key))
            val replacementRaw = key.qualifier.toLeafExpr()
            val placeholderVersionOfTheKey = QualifiedFieldKey(placeholderQualifierKey, key.field)
            val p = makeBackreference(placeholderVersionOfTheKey)

            vars[placeholderVersionOfTheKey]?.let {
                val replacedExpr = it.replaceTypeInTree<VariableExpr<*>> { expr ->
                    when (expr.key) {
                        p -> replacementQualified
                        placeholderQualifierKey -> replacementRaw
                        else -> null
                    }
                }

                return replacedExpr.getDynExpr()
            }
        }

        // OK, now we really do have no choice
        return VariableExpr.new(makeBackreference(key))
    }
}