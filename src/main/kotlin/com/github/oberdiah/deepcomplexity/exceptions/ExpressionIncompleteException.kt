package com.github.oberdiah.deepcomplexity.exceptions

/**
 * Maybe we want to handle this more elegantly in the future, but this
 * is at the very least a tidy way to group this class of failures together.
 */
class ExpressionIncompleteException(val details: String = "") : Exception() {
    override val message: String
        get() = "Statement or expression is incomplete, or the parsing has failed in some other way." +
                if (details.isNotEmpty()) " Details: $details" else ""
}