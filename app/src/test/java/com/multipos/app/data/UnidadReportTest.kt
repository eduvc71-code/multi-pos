package com.multipos.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class UnidadReportTest {

    @Test
    fun cantidadCategoriasEnUnidades() {
        val unidad = listOf(
            ReportsRepository.CAT_CANTIDAD,
            ReportsRepository.CAT_ANULADAS_COUNT,
            ReportsRepository.CAT_SESIONES,
            ReportsRepository.CAT_STOCK_ACTUAL,
            ReportsRepository.CAT_STOCK_BAJO,
            ReportsRepository.CAT_ENTRADAS,
            ReportsRepository.CAT_SALIDAS,
            ReportsRepository.CAT_MOVIMIENTOS,
            ReportsRepository.CAT_CLIENTES_SALDO
        )
        unidad.forEach { assertEquals("$it debe ser UNIDADES", Unidad.UNIDADES, ReportsRepository.unidadDe(it)) }
    }

    @Test
    fun dineroCategoriasEnMoneda() {
        val dinero = listOf(
            ReportsRepository.CAT_BRUTO,
            ReportsRepository.CAT_DESCUENTOS,
            ReportsRepository.CAT_IMPUESTOS,
            ReportsRepository.CAT_NETO,
            ReportsRepository.CAT_DEVOLUCIONES,
            ReportsRepository.CAT_INGRESO_NETO,
            ReportsRepository.CAT_COSTOS,
            ReportsRepository.CAT_GANANCIA,
            ReportsRepository.CAT_APERTURA,
            ReportsRepository.CAT_INGRESOS,
            ReportsRepository.CAT_EGRESOS,
            ReportsRepository.CAT_ESPERADO,
            ReportsRepository.CAT_CONTADO,
            ReportsRepository.CAT_DIFERENCIA,
            ReportsRepository.CAT_VALOR_COSTO,
            ReportsRepository.CAT_CARTERA,
            ReportsRepository.CAT_VENTAS_CREDITO,
            ReportsRepository.CAT_ABONOS,
            ReportsRepository.CAT_VENTAS,
            ReportsRepository.CAT_ANULADAS,
            ReportsRepository.CAT_CREDITO_ANULACION
        )
        dinero.forEach { assertEquals("$it debe ser MONEDA", Unidad.MONEDA, ReportsRepository.unidadDe(it)) }
    }
}