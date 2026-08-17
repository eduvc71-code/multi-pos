package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoInventario
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
class ReturnRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var returnRepository: ReturnRepository
    private var ownerId: Int = 0
    private var cashierId: Int = 0
    private var productId: Int = 0
    private var secondProductId: Int = 0
    private var cashSessionId: Long = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.openHelper.writableDatabase.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_caja_sesiones_una_abierta_por_empresa " +
                "ON caja_sesiones(empresaId) WHERE estado = 'ABIERTA'"
        )
        database.empresaDao().insert(Empresa(COMPANY, "Empresa A"))
        ownerId = createUser("owner", Usuario.ROL_PROPIETARIO)
        createUser("admin", Usuario.ROL_ADMINISTRADOR)
        cashierId = createUser("cashier", Usuario.ROL_CAJERO)
        productId = database.productoDao().insert(
            com.multipos.app.data.entities.Producto(
                nombre = "Producto 1",
                codigo = "P1",
                precioVenta = 1_000,
                costoUnitario = 500,
                stock = 10,
                stockMinimo = 1,
                categoria = "General",
                fotoUrl = "",
                empresaId = COMPANY
            )
        ).toInt()
        secondProductId = database.productoDao().insert(
            com.multipos.app.data.entities.Producto(
                nombre = "Producto 2",
                codigo = "P2",
                precioVenta = 2_000,
                costoUnitario = 800,
                stock = 5,
                stockMinimo = 1,
                categoria = "General",
                fotoUrl = "",
                empresaId = COMPANY
            )
        ).toInt()
        cashSessionId = CashRepository(database)
            .openSession(COMPANY, ownerId, 10_000, now = 1_000L).id
        returnRepository = ReturnRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun ownerAnnulsEffectiveSaleRestoresStockAndRecordsReversal() = runBlocking {
        val saleId = registerEffectiveSale()
        val sale = database.ventaDao().getById(saleId, COMPANY)!!

        val result = returnRepository.annulSale(
            AnnulSaleRequest(COMPANY, saleId, ownerId, "Venta registrada por error", false)
        )

        assertEquals(1, result.restoredProductCount)
        assertTrue(result.cashMovementId != null)
        assertEquals(Venta.ESTADO_ANULADA, database.ventaDao().getById(saleId, COMPANY)!!.estado)
        assertEquals(10, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)

        val movements = database.movimientoInventarioDao().getByProduct(COMPANY, productId)
        assertEquals(1, movements.size)
        assertEquals(MovimientoInventario.TIPO_ANULACION, movements[0].tipo)
        assertEquals(2, movements[0].cantidadFirmada)
        assertEquals(8, movements[0].stockAnterior)
        assertEquals(10, movements[0].stockPosterior)

        val cashMovements = database.movimientoCajaDao().getBySession(cashSessionId, COMPANY)
        assertTrue(cashMovements.any { it.tipo == MovimientoCaja.TIPO_REVERSO_ANULACION && it.monto == sale.total })
    }

    @Test
    fun annulmentRequiresPermission() = runBlocking {
        val saleId = registerEffectiveSale()

        val error = runCatching {
            returnRepository.annulSale(
                AnnulSaleRequest(COMPANY, saleId, cashierId, "Intento sin permiso", false)
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.NotAuthorized)
    }

    @Test
    fun annulmentRequiresOpenCashForCashSales() = runBlocking {
        val saleId = registerEffectiveSale()
        CashRepository(database).closeSession(COMPANY, cashSessionId, ownerId, 12_000, "Cierre de prueba")

        val error = runCatching {
            returnRepository.annulSale(
                AnnulSaleRequest(COMPANY, saleId, ownerId, "Reversión sin caja", false)
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.NoActiveCashSession)
    }

    @Test
    fun annulmentRequiresOpenCashForCreditSales() = runBlocking {
        val clientId = createCreditClient()
        database.clienteDao().increaseCredit(clientId, 2_000, COMPANY)
        val saleId = insertCreditSale(clientId, total = 2_000)
        CashRepository(database).closeSession(COMPANY, cashSessionId, ownerId, 12_000, "Cierre de prueba")

        val error = runCatching {
            returnRepository.annulSale(
                AnnulSaleRequest(COMPANY, saleId, ownerId, "Reversión de crédito sin caja", false)
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.NoActiveCashSession)
    }

    @Test
    fun annulmentRejectsPastAndFutureSalesRegardlessOfClock() = runBlocking {
        val fixedNow = 1_750_000_000_000L
        val repo = ReturnRepository(database) { fixedNow }

        val yesterdayId = insertBareSale(fixedNow - 86_400_000L)
        val pastError = runCatching {
            repo.annulSale(AnnulSaleRequest(COMPANY, yesterdayId, ownerId, "Venta anterior no anulable", false))
        }.exceptionOrNull()
        assertTrue(pastError is ReturnException.SaleNotToday)

        val futureId = insertBareSale(fixedNow + 86_400_000L)
        val futureError = runCatching {
            repo.annulSale(AnnulSaleRequest(COMPANY, futureId, ownerId, "Venta futura no anulable", false))
        }.exceptionOrNull()
        assertTrue(futureError is ReturnException.SaleNotToday)
    }

    @Test
    fun partialRefundProratesAndRecordsExpense() = runBlocking {
        val saleId = registerEffectiveSale()

        val result = returnRepository.refundSale(
            RefundSaleRequest(
                companyId = COMPANY,
                saleId = saleId,
                userId = ownerId,
                motivo = "El cliente devolvió una unidad",
                externalRefundConfirmed = false,
                lines = listOf(RefundLineRequest(detailId = 1, quantity = 1))
            )
        )

        assertEquals(1_000L, result.refundMonto)
        assertEquals(1, result.restoredProductCount)
        val sale = database.ventaDao().getById(saleId, COMPANY)!!
        assertEquals(Venta.ESTADO_COMPLETADA, sale.estado)
        assertEquals(9, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)

        val refund = database.devolucionDao().getBySale(COMPANY, saleId).single()
        assertEquals(1_000L, refund.monto)
        assertEquals("EFECTIVO", refund.medioReembolso)
        assertEquals(com.multipos.app.data.entities.Devolucion.ESTADO_COMPLETADO, refund.estadoReembolso)
        assertEquals(cashSessionId, refund.cajaSesionId)
        val refundDetail = database.devolucionDao().getDetails(refund.id).single()
        assertEquals(1, refundDetail.cantidad)
        assertEquals(1_000L, refundDetail.subtotal)

        val cashMovements = database.movimientoCajaDao().getBySession(cashSessionId, COMPANY)
        assertTrue(cashMovements.any {
            it.tipo == MovimientoCaja.TIPO_EGRESO_DEVOLUCION && it.monto == 1_000L && it.devolucionId == refund.id
        })
    }

    @Test
    fun refundCannotExceedSoldQuantity() = runBlocking {
        val saleId = registerEffectiveSale()

        val error = runCatching {
            returnRepository.refundSale(
                RefundSaleRequest(
                    companyId = COMPANY,
                    saleId = saleId,
                    userId = ownerId,
                    motivo = "Devolución de más unidades",
                    externalRefundConfirmed = false,
                    lines = listOf(RefundLineRequest(detailId = 1, quantity = 3))
                )
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.RefundExceedsSoldQuantity)
        assertEquals(8, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
    }

    @Test
    fun annulmentRequiresSameDay() = runBlocking {
        val saleId = database.ventaDao().insert(
            Venta(
                fecha = System.currentTimeMillis() - 48 * 60 * 60 * 1000L,
                tipoPago = "EFECTIVO",
                total = 1_000,
                subtotal = 1_000,
                descuento = 0,
                impuesto = 0,
                idUsuario = ownerId,
                empresaId = COMPANY
            )
        ).toInt()

        val error = runCatching {
            returnRepository.annulSale(
                AnnulSaleRequest(COMPANY, saleId, ownerId, "Venta antigua que no se anula", false)
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.SaleNotToday)
    }

    @Test
    fun creditAnnulmentReversesDebt() = runBlocking {
        val clientId = createCreditClient()
        database.clienteDao().increaseCredit(clientId, 2_000, COMPANY)
        val saleId = insertCreditSale(clientId, total = 2_000)

        returnRepository.annulSale(
            AnnulSaleRequest(COMPANY, saleId, ownerId, "Crédito otorgado por error", false)
        )

        assertEquals(Venta.ESTADO_ANULADA, database.ventaDao().getById(saleId, COMPANY)!!.estado)
        assertEquals(0L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
    }

    @Test
    fun creditAnnulmentFailsWhenDebtIsPaidDown() = runBlocking {
        val clientId = createCreditClient()
        database.clienteDao().increaseCredit(clientId, 2_000, COMPANY)
        database.clienteDao().decreaseCredit(clientId, 1_500, COMPANY)
        val saleId = insertCreditSale(clientId, total = 2_000)

        val error = runCatching {
            returnRepository.annulSale(
                AnnulSaleRequest(COMPANY, saleId, ownerId, "La deuda ya fue abonada", false)
            )
        }.exceptionOrNull()

        assertTrue(error is ReturnException.CreditDebtNotEnough)
    }

    @Test
    fun creditRefundReducesDebt() = runBlocking {
        val clientId = createCreditClient()
        database.clienteDao().increaseCredit(clientId, 2_000, COMPANY)
        val saleId = insertCreditSale(clientId, total = 2_000)

        val result = returnRepository.refundSale(
            RefundSaleRequest(
                companyId = COMPANY,
                saleId = saleId,
                userId = ownerId,
                motivo = "Devolución parcial de la compra a crédito",
                externalRefundConfirmed = false,
                lines = listOf(RefundLineRequest(detailId = 1, quantity = 1))
            )
        )

        assertEquals(1_000L, result.refundMonto)
        assertEquals(1_000L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
        val refund = database.devolucionDao().getBySale(COMPANY, saleId).single()
        assertEquals("CREDITO", refund.medioReembolso)
        assertEquals(com.multipos.app.data.entities.Devolucion.ESTADO_COMPLETADO, refund.estadoReembolso)
    }

    private suspend fun registerEffectiveSale(): Int {
        return SaleRepository(database).register(
            RegisterSaleRequest(
                paymentType = "EFECTIVO",
                total = 2_000,
                subtotal = 2_000,
                discount = 0,
                tax = 0,
                clientId = null,
                credentialId = null,
                pin = null,
                userId = ownerId,
                companyId = COMPANY,
                lines = listOf(SaleLineSnapshot(productId = productId, quantity = 2, unitPrice = 1_000))
            )
        )
    }

    private suspend fun createCreditClient(): Int {
        return database.clienteDao().insert(
            Cliente(
                nombre = "Cliente Crédito",
                documento = "DOC-001",
                limiteCredito = 10_000,
                creditoActual = 0,
                creditoHabilitado = true,
                estadoCredito = Cliente.ESTADO_ACTIVO,
                empresaId = COMPANY
            )
        ).toInt()
    }

    private suspend fun insertBareSale(fecha: Long): Int {
        return database.ventaDao().insert(
            Venta(
                fecha = fecha,
                tipoPago = "EFECTIVO",
                total = 1_000,
                subtotal = 1_000,
                descuento = 0,
                impuesto = 0,
                idUsuario = ownerId,
                empresaId = COMPANY
            )
        ).toInt()
    }

    private suspend fun insertCreditSale(clientId: Int, total: Long): Int {
        val saleId = database.ventaDao().insert(
            Venta(
                tipoPago = "CREDITO",
                total = total,
                subtotal = total,
                descuento = 0,
                impuesto = 0,
                idCliente = clientId,
                idUsuario = ownerId,
                empresaId = COMPANY
            )
        ).toInt()
        database.ventaDao().insertDetalles(
            listOf(
                DetalleVenta(
                    idVenta = saleId,
                    idProducto = productId,
                    cantidad = 2,
                    precioUnitario = 1_000,
                    subtotal = 2_000,
                    costoUnitario = 500,
                    nombreProductoSnapshot = "Producto 1",
                    empresaId = COMPANY
                )
            )
        )
        return saleId
    }

    private suspend fun createUser(username: String, role: String): Int {
        val userId = database.usuarioDao().insert(
            Usuario(nombre = username, usuario = username, rol = role, empresaId = COMPANY)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, COMPANY, role))
        return userId
    }

    private companion object {
        const val COMPANY = "returns-company"
    }
}
