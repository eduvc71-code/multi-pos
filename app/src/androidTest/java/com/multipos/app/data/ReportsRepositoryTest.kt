package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.CajaSesion
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.DetalleDevolucion
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Devolucion
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoCredito
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReportsRepositoryTest {

    private lateinit var database: AppDatabase
    private lateinit var repository: ReportsRepository
    private var adminId: Int = 0
    private var adminBId: Int = 0
    private var cashierId: Int = 0
    private var vendorId: Int = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.empresaDao().insert(Empresa(COMPANY_A, "Empresa A"))
        database.empresaDao().insert(Empresa(COMPANY_B, "Empresa B"))
        adminId = createUser("admin", Usuario.ROL_ADMINISTRADOR, COMPANY_A)
        cashierId = createUser("cashier", Usuario.ROL_CAJERO, COMPANY_A)
        vendorId = createUser("vendor", Usuario.ROL_VENDEDOR, COMPANY_A)
        adminBId = createUser("adminB", Usuario.ROL_ADMINISTRADOR, COMPANY_B)
        repository = ReportsRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun ventasReportAggregatesAndIsolatesByCompany() = runBlocking {
        insertVenta(COMPANY_A, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_000L)
        insertVenta(COMPANY_A, "TARJETA", 2_000L, cashierId, Venta.ESTADO_COMPLETADA, fecha = 2_000_000L, subtotal = 2_000L)
        insertVenta(COMPANY_B, "EFECTIVO", 9_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 3_000_000L, subtotal = 9_000L)

        val dataA = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS)
        assertEquals(3_000L, dataA.summary.total(ReportsRepository.CAT_NETO))
        assertEquals(2L, dataA.summary.total(ReportsRepository.CAT_CANTIDAD))
        assertTrue(dataA.rows.none { it.concepto.contains("#9") || it.importe == 9_000L })

        val dataB = repository.compute(COMPANY_B, adminBId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS)
        assertEquals(9_000L, dataB.summary.total(ReportsRepository.CAT_NETO))
    }

    @Test
    fun anuladasAreNotIncomeAndDevolucionesReduceNeto() = runBlocking {
        insertVenta(COMPANY_A, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_300L, descuento = 200L, impuesto = 100L)
        insertVenta(COMPANY_A, "EFECTIVO", 700L, adminId, Venta.ESTADO_ANULADA, fecha = 2_000_000L, subtotal = 700L)
        insertDevolucion(COMPANY_A, 100L)

        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS)
        // dato real: dos ventas completadas (1300 y 1000) y una anulada (700) + 1 devolución de 100
        assertEquals(2L, data.summary.total(ReportsRepository.CAT_CANTIDAD))
        assertEquals(2_300L, data.summary.total(ReportsRepository.CAT_BRUTO))
        assertEquals(200L, data.summary.total(ReportsRepository.CAT_DESCUENTOS))
        assertEquals(100L, data.summary.total(ReportsRepository.CAT_IMPUESTOS))
        assertEquals(1_900L, data.summary.total(ReportsRepository.CAT_NETO))
        assertEquals(-100L, data.summary.total(ReportsRepository.CAT_DEVOLUCIONES))
        assertEquals(1L, data.summary.total(ReportsRepository.CAT_ANULADAS_COUNT))
        assertTrue(data.rows.any { it.categoria == ReportsRepository.CAT_ANULADAS && it.importe == -700L })
    }

    @Test
    fun rentabilidadSoloCuentaUnidadesNoDevueltas() = runBlocking {
        val productId = database.productoDao().insert(
            Producto(nombre = "P1", codigo = "P1", precioVenta = 1000L, costoUnitario = 300L, stock = 10, empresaId = COMPANY_A, activo = true)
        ).toInt()
        val ventaId = insertVentaGetId(COMPANY_A, "EFECTIVO", 4_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 4_000L)
        val detailIds = database.ventaDao().insertDetalles(
            listOf(DetalleVenta(idVenta = ventaId, idProducto = productId, cantidad = 4, precioUnitario = 1_000L, subtotal = 4_000L, costoUnitario = 300L, empresaId = COMPANY_A))
        )
        // devolvemos 1 unidad: costo cuenta solo 3 unidades = 900
        insertDevolucionConDetalle(COMPANY_A, ventaId, detailIds.first().toInt(), productId, cantidadDevuelta = 1, monto = 1_000L)

        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.RENTABILIDAD)
        // 4000 venta − 1000 devolución = 3000 ingreso neto; costo 3 unidades × 300 = 900; ganancia = 3000 − 900 = 2100
        assertEquals(3_000L, data.summary.total(ReportsRepository.CAT_INGRESO_NETO))
        assertEquals(900L, data.summary.total(ReportsRepository.CAT_COSTOS))
        assertEquals(2_100L, data.summary.total(ReportsRepository.CAT_GANANCIA))
        assertTrue(ReportsRepository.FLAG_COSTO_APROXIMADO !in data.flags)
    }

    @Test
    fun costoAproximadoSeMarcaCuandoCostoEsCero() = runBlocking {
        val productId = database.productoDao().insert(
            Producto(nombre = "P2", codigo = "C2", precioVenta = 800L, costoUnitario = 0L, stock = 5, empresaId = COMPANY_A, activo = true)
        ).toInt()
        val ventaId = insertVentaGetId(COMPANY_A, "EFECTIVO", 1_600L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_600L)
        database.ventaDao().insertDetalles(
            listOf(DetalleVenta(idVenta = ventaId, idProducto = productId, cantidad = 2, precioUnitario = 800L, subtotal = 1_600L, costoUnitario = 0L, empresaId = COMPANY_A))
        )
        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.RENTABILIDAD)
        assertTrue(ReportsRepository.FLAG_COSTO_APROXIMADO in data.flags)
    }

    @Test
    fun cajaRespetaAperturaYNoCuentaAPERTURAComoEgreso() = runBlocking {
        val sesion = database.cajaSesionDao().insert(
            CajaSesion(empresaId = COMPANY_A, abiertaPorUsuarioId = adminId, fechaApertura = 1_000_000L, montoApertura = 500L, estado = CajaSesion.ESTADO_ABIERTA)
        )
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_APERTURA, monto = 500L, concepto = "Apertura", fecha = 1_000_000L))
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_INGRESO_VENTA, monto = 1_000L, concepto = "Venta", fecha = 1_100_000L))
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_EGRESO_MANUAL, monto = 200L, concepto = "Gasto", fecha = 1_200_000L))

        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.CAJA)
        assertEquals(1L, data.summary.total(ReportsRepository.CAT_SESIONES))
        assertEquals(500L, data.summary.total(ReportsRepository.CAT_APERTURA))
        assertEquals(1_000L, data.summary.total(ReportsRepository.CAT_INGRESOS))
        assertEquals(-200L, data.summary.total(ReportsRepository.CAT_EGRESOS))
        assertEquals(1_300L, data.summary.total(ReportsRepository.CAT_ESPERADO))
    }

    @Test
    fun inventarioCalculaStockValorYProductosBajos() = runBlocking {
        database.productoDao().insert(
            Producto(nombre = "P1", codigo = "C1", precioVenta = 1000L, costoUnitario = 200L, stock = 3, stockMinimo = 5, empresaId = COMPANY_A, activo = true)
        )
        database.productoDao().insert(
            Producto(nombre = "P2", codigo = "C2", precioVenta = 2000L, costoUnitario = 400L, stock = 10, stockMinimo = 5, empresaId = COMPANY_A, activo = true)
        )
        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.INVENTARIO)
        assertEquals(13L, data.summary.total(ReportsRepository.CAT_STOCK_ACTUAL))
        assertEquals(4_600L, data.summary.total(ReportsRepository.CAT_VALOR_COSTO))
        assertEquals(1L, data.summary.total(ReportsRepository.CAT_STOCK_BAJO))
    }

    @Test
    fun creditoNoDuplicaAbonos() = runBlocking {
        val clientId = database.clienteDao().insert(
            Cliente(nombre = "C", documento = "D", creditoActual = 3_000L, empresaId = COMPANY_A)
        ).toInt()
        val abonoId = database.abonoDao().insert(
            Abono(empresaId = COMPANY_A, idCliente = clientId, usuarioId = adminId, monto = 1_000L, medioPago = Abono.MEDIO_TARJETA, fecha = 1_000_000L)
        )
        val ventaId = insertVentaGetId(COMPANY_A, "CREDITO", 5_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 900_000L, subtotal = 5_000L)
        database.movimientoCreditoDao().insert(MovimientoCredito(empresaId = COMPANY_A, clienteId = clientId, usuarioId = adminId, tipo = MovimientoCredito.TIPO_VENTA_CREDITO, importeFirmado = 4_000L, saldoPosterior = 4_000L, ventaId = ventaId, fecha = 900_000L))
        database.movimientoCreditoDao().insert(MovimientoCredito(empresaId = COMPANY_A, clienteId = clientId, usuarioId = adminId, tipo = MovimientoCredito.TIPO_ABONO, importeFirmado = -1_000L, saldoPosterior = 3_000L, abonoId = abonoId, fecha = 1_000_000L))

        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.CREDITO)
        assertEquals(3_000L, data.summary.total(ReportsRepository.CAT_CARTERA))
        assertEquals(4_000L, data.summary.total(ReportsRepository.CAT_VENTAS_CREDITO))
        assertEquals(-1_000L, data.summary.total(ReportsRepository.CAT_ABONOS))
        assertEquals(1L, data.rows.count { it.categoria == ReportsRepository.CAT_ABONOS })
    }

    @Test
    fun creditoFiltraAbonoPorMedioDePago() = runBlocking {
        val clientId = database.clienteDao().insert(
            Cliente(nombre = "C", documento = "D", creditoActual = 3_000L, empresaId = COMPANY_A)
        ).toInt()
        val abonoTarjeta = database.abonoDao().insert(
            Abono(empresaId = COMPANY_A, idCliente = clientId, usuarioId = adminId, monto = 1_000L, medioPago = Abono.MEDIO_TARJETA, fecha = 1_000_000L)
        )
        val abonoEfectivo = database.abonoDao().insert(
            Abono(empresaId = COMPANY_A, idCliente = clientId, usuarioId = adminId, monto = 2_000L, medioPago = Abono.MEDIO_EFECTIVO, fecha = 1_100_000L)
        )
        database.movimientoCreditoDao().insert(MovimientoCredito(empresaId = COMPANY_A, clienteId = clientId, usuarioId = adminId, tipo = MovimientoCredito.TIPO_ABONO, importeFirmado = -1_000L, saldoPosterior = 9_000L, abonoId = abonoTarjeta, fecha = 1_000_000L))
        database.movimientoCreditoDao().insert(MovimientoCredito(empresaId = COMPANY_A, clienteId = clientId, usuarioId = adminId, tipo = MovimientoCredito.TIPO_ABONO, importeFirmado = -2_000L, saldoPosterior = 7_000L, abonoId = abonoEfectivo, fecha = 1_100_000L))

        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.CREDITO, medioPago = Abono.MEDIO_TARJETA)
        assertEquals(-1_000L, data.summary.total(ReportsRepository.CAT_ABONOS))
        assertEquals(1L, data.rows.count { it.categoria == ReportsRepository.CAT_ABONOS })
    }

    @Test
    fun paymentMethodFilterRestrictsSales() = runBlocking {
        insertVenta(COMPANY_A, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_000L)
        insertVenta(COMPANY_A, "TARJETA", 2_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 2_000_000L, subtotal = 2_000L)
        val data = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS, medioPago = "TARJETA")
        assertEquals(1L, data.rows.size.toLong())
        assertEquals(2_000L, data.summary.total(ReportsRepository.CAT_NETO))
    }

    @Test
    fun cajaExcluyeMovimientosFueraDelPeriodo() = runBlocking {
        val desde = 1_050_000L
        val hastaExclusive = 1_150_000L
        val sesion = database.cajaSesionDao().insert(
            CajaSesion(empresaId = COMPANY_A, abiertaPorUsuarioId = adminId, fechaApertura = 1_000_000L, montoApertura = 500L, estado = CajaSesion.ESTADO_ABIERTA)
        )
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_APERTURA, monto = 500L, concepto = "Apertura", fecha = 1_000_000L))
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_INGRESO_VENTA, monto = 1_000L, concepto = "Venta en rango", fecha = 1_100_000L))
        // exactamente en hastaExclusive -> NO se cuenta
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_EGRESO_MANUAL, monto = 300L, concepto = "Gasto en límite", fecha = hastaExclusive))
        // antes del inicio -> NO se cuenta
        database.movimientoCajaDao().insert(MovimientoCaja(cajaSesionId = sesion, empresaId = COMPANY_A, usuarioId = adminId, tipo = MovimientoCaja.TIPO_EGRESO_MANUAL, monto = 400L, concepto = "Gasto previo", fecha = 500_000L))

        val data = repository.compute(COMPANY_A, adminId, desde, hastaExclusive, ReporteTipo.CAJA)
        assertEquals(1L, data.summary.total(ReportsRepository.CAT_SESIONES))
        assertEquals(500L, data.summary.total(ReportsRepository.CAT_APERTURA))
        assertEquals(1_000L, data.summary.total(ReportsRepository.CAT_INGRESOS))
        assertEquals(0L, data.summary.total(ReportsRepository.CAT_EGRESOS))
        assertEquals(1_500L, data.summary.total(ReportsRepository.CAT_ESPERADO))
    }

    @Test
    fun rentabilidadExcluyeDevolucionesFueraDelPeriodo() = runBlocking {
        val hastaExclusive = 2_000_000L
        val ventaId = insertVentaGetId(COMPANY_A, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_000L)
        // devolución exactamente en hastaExclusive -> no debe afectar el periodo
        insertDevolucionPara(COMPANY_A, ventaId, monto = 500L, fecha = hastaExclusive, usuarioId = adminId)

        val data = repository.compute(COMPANY_A, adminId, 0L, hastaExclusive, ReporteTipo.RENTABILIDAD)
        assertEquals(1_000L, data.summary.total(ReportsRepository.CAT_INGRESO_NETO))
        assertEquals(0L, data.summary.total(ReportsRepository.CAT_DEVOLUCIONES))
        assertEquals(1_000L, data.summary.total(ReportsRepository.CAT_GANANCIA))
    }

    @Test
    fun rentabilidadConsideraDevolucionesDeVentasAnteriores() = runBlocking {
        val desde = 1_000_000L
        val hastaExclusive = 3_000_000L
        // venta anterior (fuera del rango) con costo 300/unidad
        val productViejo = database.productoDao().insert(
            Producto(nombre = "PV", codigo = "PV", precioVenta = 1000L, costoUnitario = 300L, stock = 10, empresaId = COMPANY_A, activo = true)
        ).toInt()
        val ventaVieja = insertVentaGetId(COMPANY_A, "EFECTIVO", 4_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 500_000L, subtotal = 4_000L)
        val detalleViejo = database.ventaDao().insertDetalles(
            listOf(DetalleVenta(idVenta = ventaVieja, idProducto = productViejo, cantidad = 4, precioUnitario = 1_000L, subtotal = 4_000L, costoUnitario = 300L, empresaId = COMPANY_A))
        ).first().toInt()

        // venta del periodo con costo de 2000
        val productNuevo = database.productoDao().insert(
            Producto(nombre = "PN", codigo = "N", precioVenta = 5000L, costoUnitario = 1_000L, stock = 10, empresaId = COMPANY_A, activo = true)
        ).toInt()
        val ventaNueva = insertVentaGetId(COMPANY_A, "EFECTIVO", 5_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 2_000_000L, subtotal = 5_000L)
        database.ventaDao().insertDetalles(
            listOf(DetalleVenta(idVenta = ventaNueva, idProducto = productNuevo, cantidad = 2, precioUnitario = 2_500L, subtotal = 5_000L, costoUnitario = 1_000L, empresaId = COMPANY_A))
        )

        // devolución del periodo de la venta ANTERIOR (2 unidades × 300 de costo)
        val devolucionId = database.devolucionDao().insert(
            Devolucion(empresaId = COMPANY_A, ventaId = ventaVieja, usuarioId = adminId, monto = 1_000L, medioReembolso = "EFECTIVO", estadoReembolso = Devolucion.ESTADO_COMPLETADO, motivo = "x", fecha = 1_500_000L)
        )
        database.devolucionDao().insertDetails(
            listOf(DetalleDevolucion(devolucionId = devolucionId, detalleVentaId = detalleViejo, productoId = productViejo, cantidad = 2, precioUnitario = 1_000L, subtotal = 1_000L))
        )

        val data = repository.compute(COMPANY_A, adminId, desde, hastaExclusive, ReporteTipo.RENTABILIDAD)
        // ingreso neto = 5000 − 1000 devolución = 4000; costo = 2000 (nueva) − 600 (revertido) = 1400; ganancia = 2600
        assertEquals(4_000L, data.summary.total(ReportsRepository.CAT_INGRESO_NETO))
        assertEquals(1_400L, data.summary.total(ReportsRepository.CAT_COSTOS))
        assertEquals(2_600L, data.summary.total(ReportsRepository.CAT_GANANCIA))
        assertEquals(-1_000L, data.summary.total(ReportsRepository.CAT_DEVOLUCIONES))
    }

    @Test
    fun devolucionesSeFiltranPorVentaOriginal() = runBlocking {
        val ventaTarjeta = insertVentaGetId(COMPANY_A, "TARJETA", 1_000L, cashierId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_000L)
        val ventaEfectivo = insertVentaGetId(COMPANY_A, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 2_000_000L, subtotal = 1_000L)
        // las devoluciones usan un usuario distinto al vendedor de la venta original, para probar que el filtro se hace por la VENTA
        insertDevolucionPara(COMPANY_A, ventaTarjeta, monto = 200L, fecha = 1_500_000L, usuarioId = adminId)
        insertDevolucionPara(COMPANY_A, ventaEfectivo, monto = 300L, fecha = 2_500_000L, usuarioId = cashierId)

        val porVendedor = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS, vendedorId = cashierId)
        assertEquals(-200L, porVendedor.summary.total(ReportsRepository.CAT_DEVOLUCIONES))
        assertEquals(1L, porVendedor.rows.count { it.categoria == ReportsRepository.CAT_DEVOLUCIONES })

        val porMedio = repository.compute(COMPANY_A, adminId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS, medioPago = "TARJETA")
        assertEquals(-200L, porMedio.summary.total(ReportsRepository.CAT_DEVOLUCIONES))
    }

    @Test
    fun nonMemberCannotReadReports() = runBlocking {
        val outsider = database.usuarioDao().insert(
            Usuario(nombre = "fora", usuario = "outsider", rol = Usuario.ROL_CAJERO, empresaId = COMPANY_A)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(outsider, COMPANY_B, Usuario.ROL_CAJERO))
        assertTrue(runCatching { repository.compute(COMPANY_A, outsider, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS) }
            .exceptionOrNull() is ReportException)
    }

    @Test
    fun cajeroAndVendedorCannotReadReports() = runBlocking {
        val eCajero = runCatching { repository.compute(COMPANY_A, cashierId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS) }.exceptionOrNull()
        val eVendedor = runCatching { repository.compute(COMPANY_A, vendorId, RANGE_LOW, RANGE_HIGH, ReporteTipo.VENTAS) }.exceptionOrNull()
        assertTrue(eCajero is ReportException)
        assertTrue(eVendedor is ReportException)
    }

    @Test
    fun emptyRangeIsRejected() = runBlocking {
        assertTrue(runCatching { repository.compute(COMPANY_A, adminId, 100L, 100L, ReporteTipo.VENTAS) }.exceptionOrNull() is ReportException)
    }

    @Test
    fun invertedRangeIsRejected() = runBlocking {
        assertTrue(runCatching { repository.compute(COMPANY_A, adminId, RANGE_HIGH, RANGE_LOW, ReporteTipo.VENTAS) }.exceptionOrNull() is ReportException)
    }

    @Test
    fun wideRangeIsRejected() = runBlocking {
        assertTrue(
            runCatching {
                repository.compute(COMPANY_A, adminId, 0L, ReportsRepository.MAX_RANGE_DAYS * ReportsRepository.DAY_MS + 1, ReporteTipo.VENTAS)
            }.exceptionOrNull() is ReportException
        )
    }

    private suspend fun insertVenta(
        company: String,
        tipoPago: String,
        total: Long,
        usuario: Int,
        estado: String,
        fecha: Long,
        subtotal: Long = total,
        descuento: Long = 0,
        impuesto: Long = 0
    ) {
        insertVentaGetId(company, tipoPago, total, usuario, estado, fecha, subtotal, descuento, impuesto)
    }

    private suspend fun insertVentaGetId(
        company: String,
        tipoPago: String,
        total: Long,
        usuario: Int,
        estado: String,
        fecha: Long,
        subtotal: Long = total,
        descuento: Long = 0,
        impuesto: Long = 0
    ): Int {
        return database.ventaDao().insert(
            Venta(
                tipoPago = tipoPago,
                total = total,
                subtotal = subtotal,
                descuento = descuento,
                impuesto = impuesto,
                idUsuario = usuario,
                estado = estado,
                empresaId = company,
                fecha = fecha
            )
        ).toInt()
    }

    private suspend fun insertDevolucion(company: String, monto: Long) {
        val ventaId = insertVentaGetId(company, "EFECTIVO", 1_000L, adminId, Venta.ESTADO_COMPLETADA, fecha = 1_000_000L, subtotal = 1_000L)
        database.devolucionDao().insert(
            Devolucion(empresaId = company, ventaId = ventaId, usuarioId = adminId, monto = monto, medioReembolso = "EFECTIVO", estadoReembolso = Devolucion.ESTADO_COMPLETADO, motivo = "x", fecha = 1_500_000L)
        )
    }

    private suspend fun insertDevolucionConDetalle(company: String, ventaId: Int, detalleVentaId: Int, productId: Int, cantidadDevuelta: Int, monto: Long) {
        val devolucionId = database.devolucionDao().insert(
            Devolucion(empresaId = company, ventaId = ventaId, usuarioId = adminId, monto = monto, medioReembolso = "EFECTIVO", estadoReembolso = Devolucion.ESTADO_COMPLETADO, motivo = "x", fecha = 1_500_000L)
        )
        database.devolucionDao().insertDetails(
            listOf(
                DetalleDevolucion(devolucionId = devolucionId, detalleVentaId = detalleVentaId, productoId = productId, cantidad = cantidadDevuelta, precioUnitario = monto, subtotal = monto)
            )
        )
    }

    private suspend fun insertDevolucionPara(company: String, ventaId: Int, monto: Long, fecha: Long, usuarioId: Int) {
        database.devolucionDao().insert(
            Devolucion(empresaId = company, ventaId = ventaId, usuarioId = usuarioId, monto = monto, medioReembolso = "EFECTIVO", estadoReembolso = Devolucion.ESTADO_COMPLETADO, motivo = "x", fecha = fecha)
        )
    }

    private suspend fun createUser(username: String, role: String, company: String): Int {
        val id = database.usuarioDao().insert(
            Usuario(nombre = username, usuario = username, rol = role, empresaId = company)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(id, company, role))
        return id
    }

    private companion object {
        const val COMPANY_A = "report-company-a"
        const val COMPANY_B = "report-company-b"
        const val RANGE_LOW = 0L
        val RANGE_HIGH = 366L * ReportsRepository.DAY_MS - 1L
    }
}