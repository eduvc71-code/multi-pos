package com.multipos.app.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

object Money {
    private val decimalPattern = Regex("^\\d+(?:[.,]\\d{1,2})?$")

    fun parseMinorUnits(raw: String): Long? {
        val value = raw.trim()
        if (!decimalPattern.matches(value)) return null
        return runCatching {
            BigDecimal(value.replace(',', '.'))
                .setScale(2, RoundingMode.UNNECESSARY)
                .movePointRight(2)
                .longValueExact()
        }.getOrNull()
    }

    fun parsePercentageBasisPoints(raw: String): Int? {
        val value = if (raw.isBlank()) "0" else raw.trim()
        val basisPoints = parseMinorUnits(value) ?: return null
        return basisPoints.takeIf { it in 0..10_000 }?.toInt()
    }

    fun calculateTax(taxableMinorUnits: Long, taxBasisPoints: Int): Long {
        require(taxableMinorUnits >= 0)
        require(taxBasisPoints in 0..10_000)
        return BigDecimal.valueOf(taxableMinorUnits)
            .multiply(BigDecimal.valueOf(taxBasisPoints.toLong()))
            .divide(BigDecimal.valueOf(10_000), 0, RoundingMode.HALF_UP)
            .longValueExact()
    }

    fun format(minorUnits: Long, locale: Locale = Locale.getDefault()): String =
        NumberFormat.getCurrencyInstance(locale).format(BigDecimal.valueOf(minorUnits, 2))

    fun toInput(minorUnits: Long): String =
        BigDecimal.valueOf(minorUnits, 2).stripTrailingZeros().toPlainString()
}
