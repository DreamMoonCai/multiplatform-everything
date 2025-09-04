package com.martmists.multiplatform.math.clifford

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.isNaN
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.truncate

/**
 * Represents a number in clifford algebra
 */
class CliffordNumber internal constructor(
    val mask: LongArray,
    val values: FloatArray,
    val algebra: CliffordAlgebra
) {
    override fun equals(other: Any?): Boolean {
        if (other !is CliffordNumber) return false
        if (this.isNaN() || other.isNaN()) return false
        withCommonAlgebra(other) { l, r ->
            return l.values.contentEquals(r.values)
        }
    }

    override fun hashCode(): Int {
        if (isNaN()) return 0
        val pairs = ArrayList<Pair<Long, Float>>(values.size)
        for (i in mask.indices) {
            val v = values[i]
            if (v == 0f) continue
            pairs.add(mask[i] to v)
        }
        pairs.sortBy { it.first }
        var result = 1
        for ((m, v) in pairs) {
            result = 31 * result + m.hashCode()
            result = 31 * result + v.hashCode()
        }
        return result
    }

    fun isRealScalar() = values.withIndex().none { (i, it) -> i != 0 && abs(it) > algebra.zeroEpsilon }
    fun scalarValue(): Float = values.zip(mask.toList()).firstOrNull { it.second == 0L }?.first ?: 0f

    operator fun plus(other: Float) = this + algebra.scalar(other)
    operator fun plus(other: CliffordNumber): CliffordNumber = withCommonAlgebra(other) { l, r ->
        val newValues = l.values.copyOf()
        for ((i, v) in r.values.withIndex()) {
            newValues[i] += v
        }
        CliffordNumber(l.mask, newValues, l.algebra)
    }

    operator fun minus(other: Float) = this - algebra.scalar(other)
    operator fun minus(other: CliffordNumber): CliffordNumber = withCommonAlgebra(other) { l, r ->
        val newValues = l.values.copyOf()
        for ((i, v) in r.values.withIndex()) {
            newValues[i] -= v
        }
        CliffordNumber(l.mask, newValues, l.algebra)
    }

    operator fun times(other: Float): CliffordNumber {
        val newValues = FloatArray(values.size) { values[it] * other }
        return CliffordNumber(mask, newValues, algebra)
    }
    operator fun times(other: CliffordNumber): CliffordNumber = withCommonAlgebra(other) { l, r ->
        val size = 1 shl l.algebra.dimension
        val resValues = FloatArray(size)

        val metric = l.algebra.metric

        val lMask = l.mask
        val rMask = r.mask

        for (i in lMask.indices) {
            val ml = lMask[i]
            val vl = l.values[i]
            if (vl == 0f) continue
            for (j in rMask.indices) {
                val mr = rMask[j]
                val vr = r.values[j]
                if (vr == 0f) continue

                var v = vl * vr
                val pm = ml xor mr
                var swaps = 0
                var tempMl = ml
                while (tempMl != 0L) {
                    val k = tempMl.countTrailingZeroBits()
                    val lowerMask = (1L shl k) - 1L
                    val count = (mr and lowerMask).countOneBits()
                    swaps += count
                    tempMl = tempMl and (tempMl - 1)
                }
                if ((swaps and 1) == 1) v = -v

                val common = ml and mr
                if (common != 0L) {
                    var tempCommon = common
                    while (tempCommon != 0L) {
                        val k = tempCommon.countTrailingZeroBits()
                        val g = metric[k]
                        v *= g
                        if (v == 0f) break
                        tempCommon = tempCommon and (tempCommon - 1)
                    }
                }
                val idx = pm.toInt()
                resValues[idx] = resValues[idx] + v
            }
        }
        val resMask = LongArray(size) { it.toLong() }
        CliffordNumber(resMask, resValues, l.algebra)
    }

    operator fun div(other: Float): CliffordNumber = this * (1 / other)
    operator fun div(other: CliffordNumber): CliffordNumber = withCommonAlgebra(other) { l, r ->
        if (other.isRealScalar()) {
            return this * (1 / other.scalarValue())
        }

        l * r.inverse()
    }

    fun pow(other: Float): CliffordNumber = this.pow(algebra.scalar(other))
    fun pow(other: CliffordNumber): CliffordNumber = exp(ln(this) * other)

    fun inverse(): CliffordNumber {
        val rev = reversed()
        var denom = (this * rev)
        if (denom.isRealScalar()) {
            return rev / denom.scalarValue()
        }
        denom *= (this * involuted()).reversed()
        check(denom.isRealScalar()) { "Could not reduce denominator to scalar, got $denom" }

        return rev / denom.scalarValue()
    }

    private fun involuted(): CliffordNumber {
        val newValues = values.copyOf()
        for (i in mask.indices) {
            val grade = mask[i].countOneBits()
            if (grade and 1 == 1) {
                newValues[i] *= -1
            }
        }
        return CliffordNumber(mask, newValues, algebra)
    }

    fun isNaN(): Boolean = values.any { it.isNaN() }

    override fun toString() = toString(4)

    private fun Float.format(digits: Int): String = when {
        isNaN() -> {
            "nan"
        }
        this == Float.POSITIVE_INFINITY -> {
            "inf"
        }
        this == Float.NEGATIVE_INFINITY -> {
            "-inf"
        }
        else -> {
            val sb = StringBuilder()
            sb.append(truncate(this).toString().dropLast(2))
            val decimal = abs(this - truncate(this))
            if (decimal > 0) {
                sb.append('.')
                sb.append((decimal * 10.0.pow(digits)).toInt().toString())
            }
            sb.toString()
        }
    }

    fun toString(digits: Int): String {
        val parts = mutableListOf<String>()
        if (isNaN()) return "nan"

        for ((m, v) in mask.toList().zip(values.toList())) {
            if (v == 0f) continue
            if (parts.isNotEmpty() && v > 0) {
                parts.add("+")
            }
            if (m == 0L) {
                parts += v.format(digits)
            } else {
                var chunk = if (v != 1f) v.format(digits) else ""
                for (d in 0 until algebra.dimension) {
                    if ((m and (1L shl d)) != 0L) {
                        chunk += "e${d+1}"
                    }
                }
                parts += chunk
            }
        }

        if (parts.isEmpty()) {
            return "0"
        }
        return parts.joinToString("")
    }

    private fun reversed(): CliffordNumber {
        val newValues = values.copyOf()
        for (i in mask.indices) {
            val m = mask[i]
            val grade = m.countOneBits()
            val sign = if (((grade * (grade - 1) / 2) and 1) == 1) -1f else 1f
            newValues[i] = newValues[i] * sign
        }
        return CliffordNumber(mask.copyOf(), newValues, algebra)
    }

    private fun reinterpretOrdered(newAlgebra: CliffordAlgebra): CliffordNumber {
        val newMask = LongArray(2 shl (newAlgebra.dimension - 1)) {
            it.toLong()
        }
        val newValues = FloatArray(newMask.size) {
            val i = mask.indexOf(it.toLong())
            if (i == -1) {
                0f
            } else {
                values[i]
            }
        }
        return CliffordNumber(newMask, newValues, newAlgebra)
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun withCommonAlgebra(other: CliffordNumber, operation: (CliffordNumber, CliffordNumber) -> CliffordNumber): CliffordNumber {
        contract {
            callsInPlace(operation, InvocationKind.EXACTLY_ONCE)
        }

        if (algebra == other.algebra) {
            return operation(reinterpretOrdered(algebra), other.reinterpretOrdered(algebra))
        } else {
            val newAlgebra = CliffordAlgebra(
                max(algebra.p, other.algebra.p),
                max(algebra.q, other.algebra.q),
                max(algebra.z, other.algebra.z),
                min(algebra.zeroEpsilon, other.algebra.zeroEpsilon),
            )
            return operation(reinterpretOrdered(newAlgebra), other.reinterpretOrdered(newAlgebra))
        }
    }
}
