package com.multipos.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.multipos.app.data.models.CartLine
import com.multipos.app.security.QrCredentialService
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReceiptPdfGenerator {

    fun mimeType(fileName: String): String = when {
        fileName.endsWith(".csv", ignoreCase = true) -> "text/csv"
        fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
        else -> "application/octet-stream"
    }

    fun createSale(context: Context, company: String, lines: List<CartLine>, subtotal: Long, discount: Long, tax: Long, total: Long, payment: String): File {
        val document = PdfDocument()
        val width = 576; val height = (430 + lines.size * 34).coerceAtLeast(760)
        val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK; textSize = 20f }
        var y = 42f
        fun line(text: String, size: Float = 20f, bold: Boolean = false) { paint.textSize = size; paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT; page.canvas.drawText(text, 32f, y, paint); y += size + 12 }
        line(company, 28f, true); line("COMPROBANTE DE VENTA", 22f, true); line(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
        line("--------------------------------", 18f)
        lines.forEach { line("${it.quantity} x ${it.product.nombre.take(25)}", 18f); line(Money.format(it.quantity * it.product.precioVenta), 18f) }
        line("--------------------------------", 18f); line("Subtotal: ${Money.format(subtotal)}", 18f); line("Descuento: ${Money.format(discount)}", 18f); line("Impuesto: ${Money.format(tax)}", 18f); line("TOTAL: ${Money.format(total)}", 25f, true); line("Pago: $payment", 18f); line("Gracias por su compra", 18f)
        document.finishPage(page)
        return save(context, document, "venta")
    }

    fun createPayment(context: Context, company: String, client: String, amount: Long, saldoAnterior: Long, saldoNuevo: Long): File {
        val document = PdfDocument(); val page = document.startPage(PdfDocument.PageInfo.Builder(576, 620, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }; var y = 48f
        fun line(text: String, size: Float = 22f, bold: Boolean = false) { paint.textSize = size; paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT; page.canvas.drawText(text, 32f, y, paint); y += size + 16 }
        line(company, 28f, true); line("COMPROBANTE DE ABONO", 22f, true); line("Fecha: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}")
        buildPaymentLines(client, amount, saldoAnterior, saldoNuevo).forEach { p -> line(p.text, p.size, p.bold) }
        line("Gracias", 20f)
        document.finishPage(page); return save(context, document, "abono")
    }

    fun buildPaymentLines(client: String, amount: Long, saldoAnterior: Long, saldoNuevo: Long): List<PdfLine> =
        listOf(
            PdfLine("Cliente: $client"),
            PdfLine("Saldo anterior: ${Money.format(saldoAnterior)}"),
            PdfLine("Monto abonado: ${Money.format(amount)}", size = 24f, bold = true),
            PdfLine("Saldo nuevo: ${Money.format(saldoNuevo)}", size = 24f, bold = true)
        )

    data class PdfLine(val text: String, val size: Float = 22f, val bold: Boolean = false)

    fun createCreditCredential(context: Context, company: String, client: String, maskedDocument: String, payload: String): File {
        val document = PdfDocument()
        val page = document.startPage(PdfDocument.PageInfo.Builder(720, 1040, 1).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.BLACK }
        var y = 70f
        fun line(text: String, size: Float = 24f, bold: Boolean = false) {
            paint.textSize = size
            paint.typeface = if (bold) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
            page.canvas.drawText(text, 48f, y, paint)
            y += size + 18
        }
        line(company, 34f, true)
        line("CREDENCIAL PRIVADA DE CRÉDITO", 25f, true)
        line(client, 28f, true)
        line("Documento: $maskedDocument", 21f)
        val qr = QrCredentialService.createBitmap(payload, 520)
        page.canvas.drawBitmap(qr, null, android.graphics.RectF(100f, y + 12f, 620f, y + 532f), paint)
        y += 580f
        line("Crédito autorizado", 25f, true)
        line("Esta credencial identifica al cliente.", 19f)
        line("No constituye un medio de pago.", 19f)
        document.finishPage(page)
        return save(context, document, "credencial_credito")
    }

    fun share(context: Context, file: File, title: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = mimeType(file.name); putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TITLE, title); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }, title))
    }

    private fun save(context: Context, document: PdfDocument, prefix: String): File { val dir = File(context.cacheDir, "receipts").apply { mkdirs() }; val file = File(dir, "${prefix}_${System.currentTimeMillis()}.pdf"); file.outputStream().use { document.writeTo(it) }; document.close(); return file }
}
