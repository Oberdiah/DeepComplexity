package com.oberdiah.deepcomplexity.context

import com.oberdiah.deepcomplexity.evaluation.Expr
import com.oberdiah.deepcomplexity.evaluation.VarsExpr
import com.oberdiah.deepcomplexity.staticAnalysis.VarsMarker

class InnerCtx(
    val flowExpr: Expr<VarsMarker>,
    val vars: Vars,
) {
    companion object {
        fun new(): InnerCtx {
            return InnerCtx(VarsExpr(), mapOf())
        }
    }

}