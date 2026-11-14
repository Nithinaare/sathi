package com.farmsaathi.core.model

import java.math.BigDecimal
import java.math.RoundingMode

object AreaConverter {
    const val MICRO_ACRES_PER_ACRE: Long = 1_000_000L
    const val GUNTAS_PER_ACRE: Int = 40
    const val CENTS_PER_ACRE: Int = 100
    const val MICRO_ACRES_PER_GUNTA: Long = MICRO_ACRES_PER_ACRE / GUNTAS_PER_ACRE
    const val MICRO_ACRES_PER_CENT: Long = MICRO_ACRES_PER_ACRE / CENTS_PER_ACRE

    fun fromAcresAndGuntas(
        acres: Int,
        guntas: Double,
    ): Long {
        require(acres >= 0 && guntas >= 0.0) { "Area cannot be negative" }
        return acres.toLong() * MICRO_ACRES_PER_ACRE + Math.round(guntas * MICRO_ACRES_PER_GUNTA)
    }

    fun fromAcresAndCents(
        acres: Int,
        cents: Double,
    ): Long {
        require(acres >= 0 && cents >= 0.0) { "Area cannot be negative" }
        return acres.toLong() * MICRO_ACRES_PER_ACRE + Math.round(cents * MICRO_ACRES_PER_CENT)
    }

    fun fromDecimalAcres(acres: BigDecimal): Long {
        require(acres >= BigDecimal.ZERO) { "Area cannot be negative" }
        return acres.multiply(BigDecimal(MICRO_ACRES_PER_ACRE))
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun toDecimalAcres(microAcres: Long): BigDecimal =
        BigDecimal(microAcres)
            .divide(BigDecimal(MICRO_ACRES_PER_ACRE), 4, RoundingMode.HALF_UP)
            .stripTrailingZeros()

    fun toAcresAndGuntas(microAcres: Long): Pair<Int, Int> {
        val totalGuntas = ((microAcres + MICRO_ACRES_PER_GUNTA / 2) / MICRO_ACRES_PER_GUNTA).toInt()
        return (totalGuntas / GUNTAS_PER_ACRE) to (totalGuntas % GUNTAS_PER_ACRE)
    }

    fun toAcresAndCents(microAcres: Long): Pair<Int, Int> {
        val totalCents = ((microAcres + MICRO_ACRES_PER_CENT / 2) / MICRO_ACRES_PER_CENT).toInt()
        return (totalCents / CENTS_PER_ACRE) to (totalCents % CENTS_PER_ACRE)
    }
}
