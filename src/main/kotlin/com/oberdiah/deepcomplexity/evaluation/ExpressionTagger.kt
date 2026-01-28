package com.oberdiah.deepcomplexity.evaluation

import com.oberdiah.deepcomplexity.evaluation.ExprToString.toStringWithTags
import com.oberdiah.deepcomplexity.utilities.Utilities.innerJoin
import com.oberdiah.deepcomplexity.utilities.Utilities.sum
import java.math.BigInteger

typealias TagsMap = Map<Expr<*>, String>

object ExpressionTagger {
    fun tagsToString(tags: TagsMap): String {
        val strBuilder = StringBuilder()
        for ((subExpr, tag) in tags) {
            strBuilder.appendLine("$tag = ${toStringWithTags(subExpr, tags - subExpr)}")
        }
        return strBuilder.toString()
    }

    /**
     * Given an expression, builds a map of the best sub-expressions for re-use, alongside names
     * for those tags. Useful for pretty-printing large expressions.
     */
    fun buildTags(expr: Expr<*>): TagsMap {
        val counts = expr.subExprCounts
        val ordering: List<Expr<*>> = ExprTreeVisitor.getTopologicalOrdering(expr).asReversed()
        val tags = mutableMapOf<Expr<*>, String>()
        val tagsSizeTakingTagsIntoAccount = mutableMapOf<Expr<*>, BigInteger>()
        ordering.forEachIndexed { index, expr ->
            // We subtract the sizes of any sub-expressions that are already tagged,
            // as those will be replaced by their tags in the final output.
            val exprSizeInReality = expr.size - expr.subExprCounts
                .innerJoin(tagsSizeTakingTagsIntoAccount)
                .values
                // Subtract 1 because the tagged expression will be replaced by a tag, which has size 1.
                .map { (count, size) -> count * (size - BigInteger.ONE) }
                .sum()

            require(exprSizeInReality > BigInteger.ZERO) {
                "Expression size was non-positive for expression: $expr"
            }

            if (counts[expr]!! > BigInteger.ONE && exprSizeInReality > BigInteger.valueOf(3)) {
                tags[expr] = "T$index"
                tagsSizeTakingTagsIntoAccount[expr] = exprSizeInReality
            }
        }

        return tags
    }
}