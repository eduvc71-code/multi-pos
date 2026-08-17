package com.multipos.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EstadoCuentaFiltersTest {

    @Test
    fun emptyInputsMeanNoFilter() {
        assertTrue(EstadoCuentaFilters.parseDateRange("", "") is EstadoCuentaFilters.RangoResult.NoFilter)
        assertTrue(EstadoCuentaFilters.parseDateRange("   ", "") is EstadoCuentaFilters.RangoResult.NoFilter)
    }

    @Test
    fun malformedDateIsInvalid() {
        assertTrue(EstadoCuentaFilters.parseDateRange("31/13/2026", "") is EstadoCuentaFilters.RangoResult.Invalid)
        assertTrue(EstadoCuentaFilters.parseDateRange("no-es-fecha", "01/01/2026") is EstadoCuentaFilters.RangoResult.Invalid)
        assertTrue(EstadoCuentaFilters.parseDateRange("", "ab-cd-ef") is EstadoCuentaFilters.RangoResult.Invalid)
    }

    @Test
    fun invertedRangeIsInvalid() {
        assertTrue(EstadoCuentaFilters.parseDateRange("05/08/2026", "01/01/2026") is EstadoCuentaFilters.RangoResult.Invalid)
        assertTrue(EstadoCuentaFilters.parseDateRange("01/01/2026", "01/01/2026") is EstadoCuentaFilters.RangoResult.Range)
    }

    @Test
    fun validRangeIsDayInclusiveFromExclusiveTo() {
        val day = EstadoCuentaFilters.startOfDay(parse("01/01/2026"))
        val range = EstadoCuentaFilters.parseDateRange("01/01/2026", "01/01/2026") as EstadoCuentaFilters.RangoResult.Range
        assertEquals(day, range.desde)
        assertEquals(day + 24 * 60 * 60 * 1000L, range.hastaExclusive)
    }

    @Test
    fun openEndedRangesExpandToMinMax() {
        val desde = parse("05/08/2026")
        val fromRange = EstadoCuentaFilters.parseDateRange("05/08/2026", "") as EstadoCuentaFilters.RangoResult.Range
        assertEquals(EstadoCuentaFilters.startOfDay(desde), fromRange.desde)
        assertEquals(Long.MAX_VALUE, fromRange.hastaExclusive)

        val hasta = parse("05/08/2026")
        val toRange = EstadoCuentaFilters.parseDateRange("", "05/08/2026") as EstadoCuentaFilters.RangoResult.Range
        assertEquals(Long.MIN_VALUE, toRange.desde)
        assertEquals(EstadoCuentaFilters.startOfDay(hasta) + 24 * 60 * 60 * 1000L, toRange.hastaExclusive)
    }

    private fun parse(date: String): Long =
        java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).parse(date)!!.time
}
