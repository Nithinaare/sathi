package com.farmsaathi.core.model

import kotlin.math.abs

/**
 * Pure integer half-up division and rounding helpers.
 * Avoids floating-point rounding drift in monetary and quantity calculations.
 */
object MathRounding {
    fun divideAndRoundHalfUp(
        numerator: Long,
        denominator: Long,
    ): Long {
        require(denominator > 0L) { "Denominator must be positive, was $denominator" }
        val quotient = numerator / denominator
        val remainder = abs(numerator % denominator)
        return if (remainder * 2L >= denominator) {
            if (numerator >= 0L) quotient + 1L else quotient - 1L
        } else {
            quotient
        }
    }
}
