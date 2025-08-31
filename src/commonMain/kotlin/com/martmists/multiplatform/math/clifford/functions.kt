package com.martmists.multiplatform.math.clifford

import kotlin.math.*

// TODO: These functions are done using taylor expansions
// Unfortunately these expansions kinda suck on large numbers, so maybe I'll need to find a better impl?

fun exp(num: CliffordNumber, terms: Int = 15): CliffordNumber {
    if (num.isRealScalar()) return num.algebra.scalar(exp(num.scalarValue()))

    var result = num.algebra.scalar(1f)
    var term = num.algebra.scalar(1f)
    for (n in 1 until terms) {
        term = term * num / n.toFloat()
        result += term
    }
    return result
}

fun ln(num: CliffordNumber, terms: Int = 15): CliffordNumber {
    if (num.isRealScalar()) return num.algebra.scalar(ln(num.scalarValue()))

    val one = num.algebra.scalar(1f)
    val x = num - one
    var result = x
    var term = x
    var sign = -1f
    for (n in 2..terms) {
        term *= x
        result += term * (sign / n.toFloat())
        sign = -sign
    }
    return result
}

fun sin(num: CliffordNumber, terms: Int = 30): CliffordNumber {
    if (num.isRealScalar()) return num.algebra.scalar(sin(num.scalarValue()))

    var result = num.algebra.scalar(0f)
    var term = num
    var sign = 1f
    for (k in 0 until terms) {
        val denom = (1..(2*k+1)).fold(1f) { acc, i -> acc * i }
        result += term * (sign / denom)
        term = term * num * num
        sign = -sign
    }
    return result
}

fun cos(num: CliffordNumber, terms: Int = 30): CliffordNumber {
    if (num.isRealScalar()) return num.algebra.scalar(cos(num.scalarValue()))

    var result = num.algebra.scalar(0f)
    var term = num.algebra.scalar(1f)
    var sign = 1f
    for (k in 0 until terms) {
        val denom = (1..(2*k)).fold(1f) { acc, i -> acc * i }
        result += term * (sign / denom)
        term = term * num * num
        sign = -sign
    }
    return result
}

fun tan(num: CliffordNumber, terms: Int = 30): CliffordNumber {
    return sin(num, terms) / cos(num, terms)
}
