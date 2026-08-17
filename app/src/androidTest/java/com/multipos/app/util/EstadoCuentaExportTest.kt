package com.multipos.app.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EstadoCuentaExportTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun csvHasUtf8BomAndHeaders() {
        val file = EstadoCuentaExport.exportCsv(
            context,
            "Mi Empresa",
            "Cliente 1",
            "01/01/2026",
            "31/01/2026",
            listOf(
                EstadoCuentaExport.Row("05/01/2026 10:00", "Venta a crédito #1", 1_000, 1_000, "Admin"),
                EstadoCuentaExport.Row("06/01/2026 11:00", "Abono", -500, 500, "Admin")
            )
        )
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.startsWith("\uFEFF"))
        assertTrue(content.contains("Empresa,Cliente,Desde,Hasta"))
        assertTrue(content.contains("Fecha,Movimiento,Importe,Saldo,Usuario"))
        assertTrue(content.contains("Mi Empresa"))
        assertTrue(content.contains("Venta a crédito #1"))
        assertTrue(content.contains(Money.format(-500)))
        assertTrue(content.contains(Money.format(1_000)))
        assertTrue(content.contains(Money.format(500)))
    }

    @Test
    fun csvEscapesQuotesAndCommas() {
        val file = EstadoCuentaExport.exportCsv(
            context,
            "Emp,resa",
            "Cliente \"Especial\", muy especial",
            "01/01/2026",
            "",
            listOf(EstadoCuentaExport.Row("05/08/2026", "Venta, con, comas", -1_000, 2_000, "admin"))
        )
        val content = file.readText(Charsets.UTF_8)
        assertTrue(content.contains("\"Emp,resa\""))
        assertTrue(content.contains("\"Cliente \"\"Especial\"\", muy especial\""))
        assertTrue(content.contains("\"Venta, con, comas\""))
    }
}