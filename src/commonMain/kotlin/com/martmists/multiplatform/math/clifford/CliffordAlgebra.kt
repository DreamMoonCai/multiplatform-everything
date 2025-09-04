package com.martmists.multiplatform.math.clifford

import kotlin.jvm.JvmStatic

/**
 * Defines a [Clifford Algebra](https://en.wikipedia.org/wiki/Clifford_algebra) system.
 */
data class CliffordAlgebra(
    val p: Int,  // (e_p)^2 == 1
    val q: Int,  // (e_q)^2 == -1
    val z: Int,  // (e_z)^2 == 0
    val zeroEpsilon: Float = 1e-6f
) {
    internal val dimension = p + q + z
    init {
        require(p >= 0 && q >= 0 && z >= 0) { "Coefficients cannot be smaller than 0" }
        require(dimension < Long.SIZE_BITS) { "The number of dimensions is too large to store in a Long, unable to instantiate algebra" }
    }
    internal val metric = FloatArray(dimension) {
        when {
            it < p -> 1f
            it < p + q -> -1f
            else -> 0f
        }
    }

    fun scalar(value: Float): CliffordNumber = CliffordNumber(longArrayOf(0L), floatArrayOf(value), this)
    fun basis(index: Int): CliffordNumber {
        require(index in 0 until dimension) { "basis $index out of bounds for Cl_{$p,$q,$z}" }
        return CliffordNumber(longArrayOf(1L shl index), floatArrayOf(1f), this)
    }

    companion object {
        @JvmStatic
        val COMPLEX = CliffordAlgebra(0, 1, 0)
        @JvmStatic
        val SPLIT_COMPLEX = CliffordAlgebra(1, 0, 0)
        @JvmStatic
        val DUAL = CliffordAlgebra(0, 0, 1)
        @JvmStatic
        val QUATERNION = CliffordAlgebra(0, 2, 0)
    }
}
