package com.multipos.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object EstadoCuentaFilters {

    sealed class RangoResult {
        object NoFilter : RangoResult()
        object Invalid : RangoResult()
        data class Range(val desde: Long, val hastaExclusive: Long) : RangoResult()
    }

    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun parseDateRange(desdeText: String, hastaText: String): RangoResult {
        val desdeRaw = desdeText.trim()
        val hastaRaw = hastaText.trim()
        if (desdeRaw.isEmpty() && hastaRaw.isEmpty()) return RangoResult.NoFilter
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).apply { isLenient = false }
        val desde = if (desdeRaw.isEmpty()) null else parseStrict(fmt, desdeRaw)
        val hasta = if (hastaRaw.isEmpty()) null else parseStrict(fmt, hastaRaw)
        if (desdeRaw.isNotEmpty() && desde == null) return RangoResult.Invalid
        if (hastaRaw.isNotEmpty() && hasta == null) return RangoResult.Invalid
        val desdeStart = desde?.let { startOfDay(it) } ?: Long.MIN_VALUE
        val hastaEnd = hasta?.let { startOfDay(it) + DAY_MS } ?: Long.MAX_VALUE
        if (desdeStart > hastaEnd) return RangoResult.Invalid
        return RangoResult.Range(desdeStart, hastaEnd)
    }

    private fun parseStrict(fmt: SimpleDateFormat, text: String): Long? =
        try { fmt.parse(text)?.time } catch (_: java.text.ParseException) { null }

    fun formatBoundary(millis: Long): String? =
        if (millis == Long.MIN_VALUE || millis == Long.MAX_VALUE) null
        else SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(millis)

    fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
