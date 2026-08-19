package com.multipos.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAggregatorTest {

    @Test
    fun aggregatesTotalsByCategory() {
        val rows = listOf(
            ReportRow(fecha = "2026-08-01", categoria = ReportsRepository.CAT_VENTAS, concepto = "Venta #1", importe = 1_000L),
            ReportRow(fecha = "2026-08-02", categoria = ReportsRepository.CAT_VENTAS, concepto = "Venta #2", importe = 2_500L),
            ReportRow(fecha = "2026-08-03", categoria = ReportsRepository.CAT_ANULADAS, concepto = "Anulación #3", importe = -500L)
        )
        val summary = ReportAggregator.aggregate(rows)
        assertEquals(3_500L, summary.total(ReportsRepository.CAT_VENTAS))
        assertEquals(-500L, summary.total(ReportsRepository.CAT_ANULADAS))
        assertEquals(0L, summary.total("INEXISTENTE"))
    }

    @Test
    fun emptyRowsProduceEmptyTotals() {
        val summary = ReportAggregator.aggregate(emptyList())
        assertTrue(summary.totals.isEmpty())
        assertEquals(0L, summary.total(ReportsRepository.CAT_VENTAS))
    }

    @Test
    fun rangeWithinLimitIsAccepted() {
        val day = ReportsRepository.DAY_MS
        assertTrue(ReportAggregator.withinLimit(0L, 366L * day))
        assertTrue(ReportAggregator.withinLimit(100L, 200L))
    }

    @Test
    fun emptyAndInvertedRangesAreRejected() {
        assertFalse(ReportAggregator.withinLimit(100L, 100L))
        assertFalse(ReportAggregator.withinLimit(200L, 100L))
        assertFalse(ReportAggregator.withinLimit(0L, 0L))
    }

    @Test
    fun rangeBeyondLimitIsRejected() {
        val day = ReportsRepository.DAY_MS
        assertFalse(ReportAggregator.withinLimit(0L, 367L * day))
        assertFalse(ReportAggregator.withinLimit(-1L, 366L * day + 1L))
    }

    @Test
    fun overflowSafeRangeComparison() {
        val day = ReportsRepository.DAY_MS
        assertFalse(ReportAggregator.withinLimit(Long.MIN_VALUE, Long.MAX_VALUE))
        assertTrue(ReportAggregator.withinLimit(1L, day))
    }
}
