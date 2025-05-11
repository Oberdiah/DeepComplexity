package com.github.oberdiah.deepcomplexity.evaluation

import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

enum class BinaryNumberOp {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION;

    override fun toString(): String {
        return when (this) {
            ADDITION -> "+"
            SUBTRACTION -> "-"
            MULTIPLICATION -> "*"
            DIVISION -> "/"
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): BinaryNumberOp? {
            return when (tokenType) {
                JavaTokenType.PLUSEQ -> ADDITION
                JavaTokenType.MINUSEQ -> SUBTRACTION
                JavaTokenType.ASTERISKEQ -> MULTIPLICATION
                JavaTokenType.DIVEQ -> DIVISION
                JavaTokenType.PLUS -> ADDITION
                JavaTokenType.MINUS -> SUBTRACTION
                JavaTokenType.ASTERISK -> MULTIPLICATION
                JavaTokenType.DIV -> DIVISION
                else -> null
            }
        }
    }
}

enum class BooleanOp {
    AND,
    OR;

    override fun toString(): String {
        return when (this) {
            AND -> "&&"
            OR -> "||"
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): BooleanOp? {
            return when (tokenType) {
                JavaTokenType.ANDAND -> AND
                JavaTokenType.OROR -> OR
                else -> null
            }
        }
    }
}

enum class ComparisonOp {
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL;

    override fun toString(): String {
        return when (this) {
            LESS_THAN -> "<"
            LESS_THAN_OR_EQUAL -> "<="
            GREATER_THAN -> ">"
            GREATER_THAN_OR_EQUAL -> ">="
        }
    }

    fun flip(): ComparisonOp {
        return when (this) {
            LESS_THAN -> GREATER_THAN
            LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL
            GREATER_THAN -> LESS_THAN
            GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): ComparisonOp? {
            return when (tokenType) {
                JavaTokenType.LT -> LESS_THAN
                JavaTokenType.LE -> LESS_THAN_OR_EQUAL
                JavaTokenType.GT -> GREATER_THAN
                JavaTokenType.GE -> GREATER_THAN_OR_EQUAL
                else -> null
            }
        }
    }
}