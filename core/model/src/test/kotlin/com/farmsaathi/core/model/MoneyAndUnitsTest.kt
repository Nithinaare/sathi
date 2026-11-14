package com.farmsaathi.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class MoneyAndUnitsTest {
    @Test
    fun `indian grouping formats lakhs and crores accurately`() {
        assertEquals("₹0", MoneyPaise.ZERO.formatInr())
        assertEquals("₹950", MoneyPaise.fromRupees(950).formatInr())
        assertEquals("₹12,500", MoneyPaise.fromRupees(12_500).formatInr())
        assertEquals("₹1,25,000", MoneyPaise.fromRupees(125_000).formatInr())
        assertEquals("₹1,05,40,250.50", MoneyPaise.fromRupeesAndPaise(10_540_250, 50).formatInr())
        assertEquals("-₹500", (-MoneyPaise.fromRupees(500)).formatInr())
    }

    @Test
    fun `parseOrNull parses rupees and paise without double drift`() {
        assertEquals(MoneyPaise(125_050L), MoneyPaise.parseOrNull("1,250.5"))
        assertEquals(MoneyPaise(125_005L), MoneyPaise.parseOrNull("₹1250.05"))
        assertEquals(MoneyPaise(100_000L), MoneyPaise.parseOrNull("1000"))
        assertEquals(MoneyPaise(10_056L), MoneyPaise.parseOrNull("100.556"))
        assertNull(MoneyPaise.parseOrNull(""))
        assertNull(MoneyPaise.parseOrNull("abc"))
    }

    @Test
    fun `machinery prorate rounds minutes to nearest whole rupee`() {
        // 90 mins at ₹1000/hr = ₹1500
        assertEquals(
            MoneyPaise.fromRupees(1500),
            MoneyPaise.prorateMinutesRoundedToRupee(90, MoneyPaise.fromRupees(1000)),
        )
        // 40 mins at ₹800/hr = 533.33 -> ₹533
        assertEquals(
            MoneyPaise.fromRupees(533),
            MoneyPaise.prorateMinutesRoundedToRupee(40, MoneyPaise.fromRupees(800)),
        )
        // 45 mins at ₹900/hr = 675 -> ₹675
        assertEquals(
            MoneyPaise.fromRupees(675),
            MoneyPaise.prorateMinutesRoundedToRupee(45, MoneyPaise.fromRupees(900)),
        )
    }

    @Test
    fun `area converter handles acres guntas and cents accurately`() {
        val micros = AreaConverter.fromAcresAndGuntas(2, 20.0)
        assertEquals(2_500_000L, micros)
        assertEquals(2 to 20, AreaConverter.toAcresAndGuntas(micros))
        assertEquals(2 to 50, AreaConverter.toAcresAndCents(micros))
        assertEquals(BigDecimal("2.5"), AreaConverter.toDecimalAcres(micros))
    }
}
