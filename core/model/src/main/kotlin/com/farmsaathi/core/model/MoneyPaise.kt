package com.farmsaathi.core.model

import kotlin.math.abs

/**
 * Authoritative INR monetary value stored strictly in integer minor units (paise).
 * Never use Double or Float for monetary arithmetic.
 */
@JvmInline
value class MoneyPaise(val paise: Long) : Comparable<MoneyPaise> {
    operator fun plus(other: MoneyPaise): MoneyPaise = MoneyPaise(Math.addExact(paise, other.paise))

    operator fun minus(other: MoneyPaise): MoneyPaise = MoneyPaise(Math.subtractExact(paise, other.paise))

    operator fun unaryMinus(): MoneyPaise = MoneyPaise(Math.negateExact(paise))

    fun isZero(): Boolean = paise == 0L

    fun isPositive(): Boolean = paise > 0L

    fun isNegative(): Boolean = paise < 0L

    override fun compareTo(other: MoneyPaise): Int = paise.compareTo(other.paise)

    fun toWholeRupeesRounded(): Long = MathRounding.divideAndRoundHalfUp(paise, PAISE_PER_RUPEE)

    fun roundToNearestRupee(): MoneyPaise = MoneyPaise(Math.multiplyExact(toWholeRupeesRounded(), PAISE_PER_RUPEE))

    fun percentage(percent: Int): MoneyPaise {
        require(percent in 0..10_000) { "Percent out of bounds: $percent" }
        return MoneyPaise(MathRounding.divideAndRoundHalfUp(Math.multiplyExact(paise, percent.toLong()), 100L))
    }

    /**
     * Formats amount using Indian digit grouping (e.g., ₹1,25,000 or ₹1,250.50).
     */
    fun formatInr(includeZeroPaise: Boolean = false): String {
        val sign = if (paise < 0L) "-" else ""
        val absVal = if (paise == Long.MIN_VALUE) Long.MAX_VALUE else abs(paise)
        val rupees = absVal / PAISE_PER_RUPEE
        val remPaise = (absVal % PAISE_PER_RUPEE).toInt()
        val groupedRupees = formatIndianGrouping(rupees)
        return if (remPaise == 0 && !includeZeroPaise) {
            "$sign₹$groupedRupees"
        } else {
            val paiseStr = remPaise.toString().padStart(2, '0')
            "$sign₹$groupedRupees.$paiseStr"
        }
    }

    companion object {
        const val CURRENCY_INR = "INR"
        const val PAISE_PER_RUPEE = 100L
        val ZERO = MoneyPaise(0L)

        fun fromRupees(rupees: Long): MoneyPaise = MoneyPaise(Math.multiplyExact(rupees, PAISE_PER_RUPEE))

        fun fromRupeesAndPaise(
            rupees: Long,
            paise: Int,
        ): MoneyPaise {
            require(paise in 0..99) { "Paise must be in 0..99, was $paise" }
            val sign = if (rupees < 0L) -1L else 1L
            return MoneyPaise(sign * (Math.multiplyExact(abs(rupees), PAISE_PER_RUPEE) + paise))
        }

        /**
         * Parses user text such as "1,250", "₹1250.5", or "1250.50" into MoneyPaise via exact integer math.
         */
        fun parseOrNull(raw: String): MoneyPaise? {
            val trimmed = raw.trim().replace(",", "").replace("₹", "").replace(" ", "")
            val negative = trimmed.startsWith("-")
            val unsigned = if (negative) trimmed.substring(1) else trimmed
            val parts = unsigned.split(".")
            val validStructure =
                unsigned.isNotEmpty() &&
                    parts.size <= 2 &&
                    parts[0].all { it.isDigit() } &&
                    (parts.size == 1 || parts[0].isNotEmpty() || parts[1].isNotEmpty())
            val rupeesPart = if (validStructure) parts[0].ifEmpty { "0" }.toLongOrNull() else null
            val paisePart = if (validStructure && parts.size == 2) parseFractionalPaise(parts[1]) else 0L

            return if (rupeesPart == null || paisePart == null) {
                null
            } else {
                val total = Math.addExact(Math.multiplyExact(rupeesPart, PAISE_PER_RUPEE), paisePart)
                MoneyPaise(if (negative) -total else total)
            }
        }

        private fun parseFractionalPaise(frac: String): Long? =
            when {
                frac.isEmpty() -> 0L
                frac.any { !it.isDigit() } -> null
                frac.length == 1 -> frac.toLong() * 10L
                frac.length == 2 -> frac.toLong()
                else -> {
                    val base = frac.substring(0, 2).toLong()
                    val nextDigit = frac[2] - '0'
                    if (nextDigit >= 5) base + 1L else base
                }
            }

        /**
         * Computes prorated duration charge in paise rounded half-up to nearest ₹1 (100 paise)
         * in a single integer division (hourlyRatePaise * durationMinutes / 6000 * 100).
         */
        fun prorateMinutesRoundedToRupee(
            durationMinutes: Long,
            hourlyRate: MoneyPaise,
        ): MoneyPaise {
            if (durationMinutes <= 0L || hourlyRate.paise <= 0L) return ZERO
            val roundedRupees =
                MathRounding.divideAndRoundHalfUp(
                    Math.multiplyExact(hourlyRate.paise, durationMinutes),
                    6_000L,
                )
            return MoneyPaise(Math.multiplyExact(roundedRupees, PAISE_PER_RUPEE))
        }

        fun multiplyMilliUnits(
            unitPrice: MoneyPaise,
            quantityMilliUnits: Long,
        ): MoneyPaise {
            if (unitPrice.paise == 0L || quantityMilliUnits == 0L) return ZERO
            return MoneyPaise(
                MathRounding.divideAndRoundHalfUp(
                    Math.multiplyExact(unitPrice.paise, quantityMilliUnits),
                    1_000L,
                ),
            )
        }

        internal fun formatIndianGrouping(value: Long): String {
            val s = value.toString()
            if (s.length <= 3) return s
            val lastThree = s.takeLast(3)
            val rest = s.dropLast(3)
            val groupedRest = rest.reversed().chunked(2).joinToString(",").reversed()
            return "$groupedRest,$lastThree"
        }
    }
}
