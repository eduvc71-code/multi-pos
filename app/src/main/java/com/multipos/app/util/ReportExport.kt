package com.multipos.app.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.multipos.app.data.ReportRow
import com.multipos.app.data.ReportsRepository
import com.multipos.app.data.ReportSummary
import com.multipos.app.data.Unidad
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExport {

    private fun format(categoria: String, importe: Long): String =
        if (ReportsRepository.unidadDe(categoria) == Unidad.MONEDA) Money.format(importe) else importe.toString()

    fun exportCsv(
        context: Context,
        company: String,
        reportName: String,
        reportKey: String,
        desde: String,
        hasta: String,
        rows: List<ReportRow>,
        summary: ReportSummary = ReportSummary(emptyMap())
    ): File {
        val sb = StringBuilder("\uFEFF")
        sb.append("Empresa,Reporte,Desde,Hasta\n")
        sb.append(csvCell(company)).append(',').append(csvCell(reportName)).append(',').append(csvCell(desde)).append(',').append(csvCell(hasta)).append('\n')
        sb.append("Fecha,Categoria,Concepto,Importe\n")
        rows.forEach { r ->
            sb.append(csvCell(r.fecha)).append(',')
                .append(csvCell(r.categoria)).append(',')
                .append(csvCell(r.concepto)).append(',')
                .append(csvCell(format(r.categoria, r.importe))).append('\n')
        }
        if (summary.totals.isNotEmpty()) {
            sb.append('\n')
            sb.append("Total,Valor\n")
            summary.totals.forEach { (cat, value) ->
                sb.append(csvCell(cat)).append(',')
                    .append(csvCell(format(cat, value))).append('\n')
            }
        }
        val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, fileName(reportKey, company, "csv"))
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun exportPdf(
        context: Context,
        company: String,
        reportName: String,
        reportKey: String,
        desde: String,
        hasta: String,
        rows: List<ReportRow>,
        summary: ReportSummary = ReportSummary(emptyMap()),
        nota: String? = null
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 12f }
        val generated = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val pageWidth = 720
        val pageHeight = 1040
        val summaryHeight = if (summary.totals.isEmpty()) 0 else (summary.totals.size * 16 + 24)
        val headerHeight = 200 + summaryHeight
        val rowHeight = 22
        val rowsPerPage = (pageHeight - headerHeight - 60) / rowHeight

        fun buildPage(startIndex: Int): Int {
            val page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create())
            val canvas = page.canvas
            var y = 50f
            fun text(line: String, size: Float = 14f, bold: Boolean = false) {
                paint.textSize = size
                paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
                canvas.drawText(line, 32f, y, paint)
                y += size + 12
            }
            text(company, 18f, true)
            text(reportName.uppercase(Locale.getDefault()), 15f, true)
            text("Periodo: $desde → $hasta")
            text("Fecha de generación: $generated")
            if (nota != null) text("Nota: $nota", 12f)
            if (summary.totals.isNotEmpty()) {
                y += 8
                paint.textSize = 12f
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                canvas.drawText("TOTALES", 32f, y, paint)
                y += 20
                paint.typeface = android.graphics.Typeface.DEFAULT
                summary.totals.forEach { (cat, value) ->
                    canvas.drawText(cat, 32f, y, small)
                    canvas.drawText(format(cat, value), 620f, y, small)
                    y += 16
                }
            }
            y += 12
            canvas.drawText("Fecha", 32f, y, paint)
            canvas.drawText("Concepto", 220f, y, paint)
            canvas.drawText("Importe", 620f, y, paint)
            y += 6
            canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, paint)
            y += 12
            var drawn = 0
            for (i in startIndex until rows.size) {
                if (drawn >= rowsPerPage) break
                val r = rows[i]
                canvas.drawText(r.fecha, 32f, y, small)
                canvas.drawText(r.concepto.take(52), 220f, y, small)
                canvas.drawText(format(r.categoria, r.importe), 620f, y, small)
                y += rowHeight
                drawn++
            }
            if (rows.isEmpty()) {
                canvas.drawText("Sin datos para el periodo", 32f, y + 20f, small)
            }
            canvas.drawText("Página ${(startIndex / (rowsPerPage.coerceAtLeast(1))) + 1}", 32f, pageHeight - 30f, small)
            document.finishPage(page)
            return drawn
        }

        if (rows.isEmpty()) {
            buildPage(0)
        } else {
            var index = 0
            while (index < rows.size) {
                index += buildPage(index)
            }
        }
        val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, fileName(reportKey, company, "pdf"))
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun fileName(reportKey: String, company: String, ext: String): String {
        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val safeCompany = company.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return "multipos_${reportKey}_${safeCompany}_$stamp.$ext"
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}