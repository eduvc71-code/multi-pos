package com.multipos.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptPdfGeneratorTest {

    @Test
    fun mimeTypeIsDerivedFromFileExtension() {
        assertEquals("application/pdf", ReceiptPdfGenerator.mimeType("comprobante.pdf"))
        assertEquals("application/pdf", ReceiptPdfGenerator.mimeType("COMPROBANTE.PDF"))
        assertEquals("text/csv", ReceiptPdfGenerator.mimeType("estado_cuenta.csv"))
        assertEquals("text/csv", ReceiptPdfGenerator.mimeType("ESTADO.CSV"))
        assertEquals("application/octet-stream", ReceiptPdfGenerator.mimeType("archivo.txt"))
        assertEquals("application/octet-stream", ReceiptPdfGenerator.mimeType("sinExtension"))
    }

    @Test
    fun paymentComprobanteLinesIncludePreviousAmountAndNewBalance() {
        val lines = ReceiptPdfGenerator.buildPaymentLines("Juan Pérez", 1_500L, 5_000L, 3_500L)
        val text = lines.joinToString("\n") { it.text }
        assertTrue(text.contains("Juan Pérez"))
        assertTrue(text.contains("Saldo anterior: " + Money.format(5_000L)))
        assertTrue(text.contains("Monto abonado: " + Money.format(1_500L)))
        assertTrue(text.contains("Saldo nuevo: " + Money.format(3_500L)))
    }

    @Test
    fun paymentComprobanteBoldsAmountAndNewBalance() {
        val lines = ReceiptPdfGenerator.buildPaymentLines("Cliente", 100L, 200L, 100L)
        val amountLine = lines.first { it.text.startsWith("Monto abonado") }
        val newBalanceLine = lines.first { it.text.startsWith("Saldo nuevo") }
        assertTrue(amountLine.bold)
        assertTrue(newBalanceLine.bold)
        assertEquals(24f, amountLine.size, 0f)
        assertEquals(24f, newBalanceLine.size, 0f)
    }
}
