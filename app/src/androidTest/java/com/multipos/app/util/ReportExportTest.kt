package com.multipos.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.ReportRow
import com.multipos.app.data.ReportSummary
import com.multipos.app.data.ReportsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportExportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun csvHasBomAndEscapesAndSummary() {
        val file = ReportExport.exportCsv(
            context,
            "Emp,resa",
            "Ventas",
            "ventas",
            "01/01/2026",
            "31/01/2026",
            listOf(
                ReportRow("05/08/2026 10:00", ReportsRepository.CAT_VENTAS, "Venta \"#1\", EFECTIVO", 1_000),
                ReportRow("06/08/2026 11:00", ReportsRepository.CAT_VENTAS, "Venta #2", -500)
            ),
            ReportSummary(mapOf(ReportsRepository.CAT_NETO to 500L))
        )
        val content = readContent(file)
        assertTrue(content.startsWith("\uFEFF"))
        assertTrue(content.contains("Empresa,Reporte,Desde,Hasta"))
        assertTrue(content.contains("Fecha,Categoria,Concepto,Importe"))
        assertTrue(content.contains("\"Emp,resa\""))
        assertTrue(content.contains("\"Venta \"\"#1\"\", EFECTIVO\""))
        assertTrue(content.contains("Total,Valor"))
        assertTrue(content.contains("NETO"))
        assertTrue(content.contains(Money.format(1_000)))
    }

    @Test
    fun exportUsesMultiposNamingConvention() {
        val csv = ReportExport.exportCsv(context, "Empresa Beta", "Ventas", "ventas", "01/01/2026", "31/01/2026", emptyList())
        val pdf = ReportExport.exportPdf(context, "Empresa Beta", "Ventas", "ventas", "01/01/2026", "31/01/2026", emptyList())
        assertTrue(csv.name.startsWith("multipos_ventas_Empresa_Beta_"))
        assertTrue(csv.name.endsWith(".csv"))
        assertTrue(pdf.name.startsWith("multipos_ventas_Empresa_Beta_"))
        assertTrue(pdf.name.endsWith(".pdf"))
    }

    @Test
    fun pdfIsGeneratedWithTotals() {
        val file = ReportExport.exportPdf(
            context,
            "Empresa",
            "Ventas",
            "ventas",
            "01/01/2026",
            "31/01/2026",
            listOf(ReportRow("05/08/2026", ReportsRepository.CAT_VENTAS, "Venta #1", 1_000)),
            ReportSummary(mapOf(ReportsRepository.CAT_NETO to 1_000L))
        )
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }

    @Test
    fun exportFormatsMoneyAndUnitsPerCategory() {
        val file = ReportExport.exportCsv(
            context,
            "Empresa",
            "Inventario",
            "inventario",
            "01/01/2026",
            "31/01/2026",
            listOf(
                ReportRow("", ReportsRepository.CAT_STOCK_ACTUAL, "Stock", 12),
                ReportRow("", ReportsRepository.CAT_ENTRADAS, "Entradas", 5)
            ),
            ReportSummary(
                mapOf(
                    ReportsRepository.CAT_VALOR_COSTO to 12_345L,
                    ReportsRepository.CAT_STOCK_BAJO to 3L
                )
            )
        )
        val content = readContent(file)
        // unidades sin formato monetario
        assertTrue(content.contains("\"Stock\",\"12\""))
        assertTrue(content.contains("\"Entradas\",\"5\""))
        assertTrue(content.contains("\"STOCK_BAJO\",\"3\""))
        // dinero con formato monetario
        assertTrue(content.contains(Money.format(12_345L)))
        assertTrue(!content.contains("\"VALOR_COSTO\",\"12345\""))
    }

    private fun readContent(file: java.io.File): String = file.readText(Charsets.UTF_8)
}