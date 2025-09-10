package com.github.oberdiah.deepcomplexity.evaluation

import com.intellij.psi.JavaTokenType
import com.intellij.psi.tree.IElementType

enum class UnaryNumberOp {
    INCREMENT,
    DECREMENT,
    NEGATE,
    PLUS; // I don't think this ever does anything other than potentially an implicit cast.

    override fun toString(): String {
        return when (this) {
            INCREMENT -> "++"
            DECREMENT -> "--"
            NEGATE -> "-"
            PLUS -> "+"
        }
    }

    fun <T : Number> applyToExpr(expr: Expr<T>): Expr<T> {
        return when (this) {
            PLUS -> expr
            NEGATE -> NegateExpression(expr)
            INCREMENT, DECREMENT -> {
                ArithmeticExpression(
                    expr,
                    ConstExpr.one(expr.getNumberSetIndicator()),
                    if (this == INCREMENT) BinaryNumberOp.ADDITION else BinaryNumberOp.SUBTRACTION
                )
            }
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): UnaryNumberOp? {
            return when (tokenType) {
                JavaTokenType.PLUSPLUS -> INCREMENT
                JavaTokenType.MINUSMINUS -> DECREMENT
                JavaTokenType.MINUS -> NEGATE
                JavaTokenType.PLUS -> PLUS
                else -> null
            }
        }
    }
}

enum class BinaryNumberOp {
    ADDITION,
    SUBTRACTION,
    MULTIPLICATION,
    DIVISION,
    MODULO;

    override fun toString(): String {
        return when (this) {
            ADDITION -> "+"
            SUBTRACTION -> "-"
            MULTIPLICATION -> "*"
            DIVISION -> "/"
            MODULO -> "%"
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): BinaryNumberOp? {
            return when (tokenType) {
                JavaTokenType.PLUSEQ -> ADDITION
                JavaTokenType.MINUSEQ -> SUBTRACTION
                JavaTokenType.ASTERISKEQ -> MULTIPLICATION
                JavaTokenType.DIVEQ -> DIVISION
                JavaTokenType.PERCEQ -> MODULO

                JavaTokenType.PLUS -> ADDITION
                JavaTokenType.MINUS -> SUBTRACTION
                JavaTokenType.ASTERISK -> MULTIPLICATION
                JavaTokenType.DIV -> DIVISION
                JavaTokenType.PERC -> MODULO
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
    GREATER_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL;

    override fun toString(): String {
        return when (this) {
            LESS_THAN -> "<"
            LESS_THAN_OR_EQUAL -> "<="
            GREATER_THAN -> ">"
            GREATER_THAN_OR_EQUAL -> ">="
            EQUAL -> "=="
            NOT_EQUAL -> "!="
        }
    }

    fun invert(): ComparisonOp {
        return when (this) {
            LESS_THAN -> GREATER_THAN_OR_EQUAL
            LESS_THAN_OR_EQUAL -> GREATER_THAN
            GREATER_THAN -> LESS_THAN_OR_EQUAL
            GREATER_THAN_OR_EQUAL -> LESS_THAN
            EQUAL -> NOT_EQUAL
            NOT_EQUAL -> EQUAL
        }
    }

    fun flip(): ComparisonOp {
        return when (this) {
            LESS_THAN -> GREATER_THAN
            LESS_THAN_OR_EQUAL -> GREATER_THAN_OR_EQUAL
            GREATER_THAN -> LESS_THAN
            GREATER_THAN_OR_EQUAL -> LESS_THAN_OR_EQUAL
            EQUAL -> EQUAL
            NOT_EQUAL -> NOT_EQUAL
        }
    }

    companion object {
        fun fromJavaTokenType(tokenType: IElementType): ComparisonOp? {
            return when (tokenType) {
                JavaTokenType.LT -> LESS_THAN
                JavaTokenType.LE -> LESS_THAN_OR_EQUAL
                JavaTokenType.GT -> GREATER_THAN
                JavaTokenType.GE -> GREATER_THAN_OR_EQUAL
                JavaTokenType.EQEQ -> EQUAL
                JavaTokenType.NE -> NOT_EQUAL
                else -> null
            }
        }
    }
}