package com.multipos.app.util

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parsesDotAndCommaDecimalsIntoMinorUnits() {
        assertEquals(1_234L, Money.parseMinorUnits("12.34"))
        assertEquals(1_234L, Money.parseMinorUnits("12,34"))
        assertEquals(1_200L, Money.parseMinorUnits("12"))
    }

    @Test
    fun rejectsNegativeValuesExtraDecimalsAndOverflow() {
        assertNull(Money.parseMinorUnits("-1.00"))
        assertNull(Money.parseMinorUnits("1.001"))
        assertNull(Money.parseMinorUnits("999999999999999999999.99"))
    }

    @Test
    fun percentageUsesBasisPointsAndValidatesRange() {
        assertEquals(1_325, Money.parsePercentageBasisPoints("13.25"))
        assertEquals(0, Money.parsePercentageBasisPoints(""))
        assertNull(Money.parsePercentageBasisPoints("100.01"))
    }

    @Test
    fun taxRoundsToNearestMinorUnitUsingHalfUp() {
        assertEquals(13L, Money.calculateTax(101, 1_300))
        assertEquals(11L, Money.calculateTax(105, 1_000))
    }

    @Test
    fun formatsAndConvertsWithoutUsingFloatingPoint() {
        assertEquals("12.34", Money.toInput(1_234))
        assertEquals("$12.34", Money.format(1_234, Locale.US))
    }
}
