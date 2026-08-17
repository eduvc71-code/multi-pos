package com.multipos.app.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EstadoCuentaExport {

    data class Row(
        val fecha: String,
        val tipo: String,
        val importeFirmado: Long,
        val saldoPosterior: Long,
        val usuario: String
    )

    fun exportCsv(
        context: Context,
        company: String,
        clientName: String,
        desde: String,
        hasta: String,
        rows: List<Row>
    ): File {
        val sb = StringBuilder("\uFEFF")
        sb.append("Empresa,Cliente,Desde,Hasta\n")
        sb.append(csvCell(company)).append(',').append(csvCell(clientName)).append(',').append(csvCell(desde)).append(',').append(csvCell(hasta)).append('\n')
        sb.append("Fecha,Movimiento,Importe,Saldo,Usuario\n")
        rows.forEach { r ->
            sb.append(csvCell(r.fecha)).append(',')
                .append(csvCell(r.tipo)).append(',')
                .append(csvCell(Money.format(r.importeFirmado))).append(',')
                .append(csvCell(Money.format(r.saldoPosterior))).append(',')
                .append(csvCell(r.usuario)).append('\n')
        }
        val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, "estado_cuenta_${System.currentTimeMillis()}.csv")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    fun exportPdf(
        context: Context,
        company: String,
        clientName: String,
        desde: String,
        hasta: String,
        rows: List<Row>
    ): File {
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
        val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 12f }
        val pageWidth = 720
        val pageHeight = 1040
        val headerHeight = 180
        val rowHeight = 22
        val rowsPerPage = (pageHeight - headerHeight - 40) / rowHeight

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
            text("ESTADO DE CUENTA · $clientName", 15f, true)
            text("Periodo: $desde → $hasta")
            text("Generado: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
            y += 12
            text("Fecha", 12f, true)
            canvas.drawText("Movimiento", 220f, y, paint)
            canvas.drawText("Importe", 470f, y, paint)
            canvas.drawText("Saldo", 570f, y, paint)
            canvas.drawText("Usuario", 640f, y, paint)
            y += 6
            canvas.drawLine(32f, y, (pageWidth - 32).toFloat(), y, paint)
            y += 12
            var drawn = 0
            for (i in startIndex until rows.size) {
                if (drawn >= rowsPerPage) break
                val r = rows[i]
                canvas.drawText(r.fecha, 32f, y, small)
                canvas.drawText(r.tipo.take(24), 220f, y, small)
                canvas.drawText(Money.format(r.importeFirmado), 470f, y, small)
                canvas.drawText(Money.format(r.saldoPosterior), 570f, y, small)
                canvas.drawText(r.usuario.take(12), 640f, y, small)
                y += rowHeight
                drawn++
            }
            if (rows.isEmpty()) {
                canvas.drawText("Sin datos para el periodo", 32f, y + 20f, small)
            }
            canvas.drawText("Página ${(startIndex / rowsPerPage) + 1}", 32f, pageHeight - 30f, small)
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
        val file = File(File(context.cacheDir, "exports").apply { mkdirs() }, "estado_cuenta_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun csvCell(value: String): String = "\"${value.replace("\"", "\"\"")}\""
}