package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.dao.ClienteDao
import com.multipos.app.data.dao.ProductoDao
import com.multipos.app.data.entities.*
import com.multipos.app.security.PinHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AtomicSaleTest {
    private lateinit var db: AppDatabase
    private lateinit var clienteDao: ClienteDao
    private lateinit var productoDao: ProductoDao
    private val compId = "test-comp"
    private val userId = 1

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        runBlocking {
            db.empresaDao().insert(Empresa(compId, "Test", "TIENDA", "#000000", null, true, "", "", "", 0))
            db.usuarioDao().insert(Usuario(id = userId, nombre = "Admin", usuario = "admin", password = "", rol = "PROPIETARIO", empresaId = compId, activo = true))
            db.cajaSesionDao().insert(
                CajaSesion(
                    empresaId = compId,
                    abiertaPorUsuarioId = userId,
                    fechaApertura = System.currentTimeMillis(),
                    montoApertura = 0,
                    estado = CajaSesion.ESTADO_ABIERTA
                )
            )
        }
        clienteDao = db.clienteDao()
        productoDao = db.productoDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun saleIsRevertedFullyIfOneProductIsOutOfStock() = runBlocking {
        // 1. Preparar datos
        val clienteId = 1
        val productoAId = 1
        val productoBId = 2
        val stockOriginalA = 10
        val stockOriginalB = 1
        val creditoOriginal = 0L
        val limiteCredito = 20000L

        clienteDao.insert(Cliente(id = clienteId, nombre = "C1", documento = "123", telefono = "", direccion = "", limiteCredito = limiteCredito, creditoActual = creditoOriginal, creditoHabilitado = true, estadoCredito = "ACTIVO", fechaInscripcion = 0, activo = true, empresaId = compId))
        productoDao.insert(Producto(id = productoAId, nombre = "P-A", codigo = "PA", precioVenta = 5000, costoUnitario = 2000, stock = stockOriginalA, stockMinimo = 0, categoria = "C", fotoUrl = "", empresaId = compId))
        productoDao.insert(Producto(id = productoBId, nombre = "P-B", codigo = "PB", precioVenta = 3000, costoUnitario = 1000, stock = stockOriginalB, stockMinimo = 0, categoria = "C", fotoUrl = "", empresaId = compId))

        val pin = "0000"
        val digest = PinHasher.hash(pin.toCharArray())
        val credentialId = "credential-test"
        db.credencialClienteDao().insert(
            CredencialCliente(
                clienteId = clienteId,
                empresaId = compId,
                credentialId = credentialId,
                emitidaPorUsuarioId = userId,
                pinHash = digest.hash,
                pinSalt = digest.salt,
                estado = "ACTIVA",
                fechaEmision = System.currentTimeMillis(),
                fechaVencimiento = System.currentTimeMillis() + 86400000L
            )
        )

        // 2. Intentar venta a crédito de 1 unidad de A (stock OK) y 2 de B (stock INSUFICIENTE)
        val ventaTotal = 5000L + (2 * 3000L) // 11000

        try {
            SaleRepository(db).register(
                RegisterSaleRequest(
                    paymentType = "CREDITO",
                    total = ventaTotal,
                    subtotal = ventaTotal,
                    discount = 0,
                    tax = 0,
                    clientId = clienteId,
                    credentialId = credentialId,
                    pin = pin,
                    userId = userId,
                    companyId = compId,
                    lines = listOf(
                        SaleLineSnapshot(productoAId, 1, 5000),
                        SaleLineSnapshot(productoBId, 2, 3000)
                    )
                )
            )
            fail("La transacción debería haber fallado por stock insuficiente")
        } catch (_: SaleRegistrationException.InsufficientStock) {
            // Resultado esperado.
        }

        // 3. Verificar que TODA la transacción se revirtió.
        val clienteFinal = clienteDao.getById(clienteId, compId)
        assertEquals("El crédito del cliente debe haberse revertido al valor original", creditoOriginal, clienteFinal?.creditoActual)

        db.query("SELECT COUNT(*) FROM ventas", null).use {
            it.moveToFirst()
            assertEquals("No debe existir la venta", 0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM detalle_ventas", null).use {
            it.moveToFirst()
            assertEquals("No deben existir detalles de venta", 0, it.getInt(0))
        }
        db.query("SELECT stock FROM productos WHERE id = $productoAId", null).use {
            it.moveToFirst()
            assertEquals("El stock del producto A debe haberse revertido", stockOriginalA, it.getInt(0))
        }
        db.query("SELECT stock FROM productos WHERE id = $productoBId", null).use {
            it.moveToFirst()
            assertEquals("El stock del producto B no debe haber cambiado", stockOriginalB, it.getInt(0))
        }
    }

    @Test
    fun successfulCashSalePersistsHeaderDetailsAndStock() = runBlocking {
        val productId = productoDao.insert(
            Producto(
                nombre = "P-A",
                codigo = "PA",
                precioVenta = 5000,
                costoUnitario = 2000,
                stock = 10,
                empresaId = compId
            )
        ).toInt()

        val saleId = SaleRepository(db).register(
            RegisterSaleRequest(
                paymentType = "EFECTIVO",
                total = 9000,
                subtotal = 10000,
                discount = 1000,
                tax = 0,
                clientId = null,
                credentialId = null,
                pin = null,
                userId = userId,
                companyId = compId,
                lines = listOf(SaleLineSnapshot(productId, 2, 5000))
            )
        )

        db.query("SELECT total, subtotal, descuento FROM ventas WHERE id = $saleId", null).use {
            it.moveToFirst()
            assertEquals(9000L, it.getLong(0))
            assertEquals(10000L, it.getLong(1))
            assertEquals(1000L, it.getLong(2))
        }
        db.query("SELECT COUNT(*), subtotal FROM detalle_ventas WHERE idVenta = $saleId", null).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(10000L, it.getLong(1))
        }
        assertEquals(8, productoDao.getByIdIncludingInactive(productId, compId)?.stock)
        db.query("SELECT cajaSesionId FROM ventas WHERE id = $saleId", null).use {
            it.moveToFirst()
            assertFalse(it.isNull(0))
        }
        db.query("SELECT COUNT(*), monto FROM movimientos_caja WHERE ventaId = $saleId", null).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
            assertEquals(9000L, it.getLong(1))
        }
    }

    @Test
    fun saleWithoutOpenCashSessionDoesNotPersistAnything() = runBlocking {
        val active = db.cajaSesionDao().getActiveSessionForCompany(compId)!!
        db.cajaSesionDao().cerrarSesion(active.id, compId, userId, System.currentTimeMillis(), 0, 0, 0, "")
        val productId = productoDao.insert(
            Producto(nombre = "P-C", codigo = "PC", precioVenta = 1000, costoUnitario = 500, stock = 2, empresaId = compId)
        ).toInt()

        try {
            SaleRepository(db).register(
                RegisterSaleRequest(
                    paymentType = "EFECTIVO",
                    total = 1000,
                    subtotal = 1000,
                    discount = 0,
                    tax = 0,
                    clientId = null,
                    credentialId = null,
                    pin = null,
                    userId = userId,
                    companyId = compId,
                    lines = listOf(SaleLineSnapshot(productId, 1, 1000))
                )
            )
            fail("La venta debía exigir una caja abierta")
        } catch (_: SaleRegistrationException.NoActiveCashSession) {
            // Resultado esperado.
        }

        assertEquals(2, productoDao.getByIdIncludingInactive(productId, compId)?.stock)
        db.query("SELECT COUNT(*) FROM ventas", null).use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
        }
    }

    @Test
    fun negativeQuantityRejectsWithoutPersistingAnything() = runBlocking {
        val productId = productoDao.insert(
            Producto(
                nombre = "P-D",
                codigo = "PD",
                precioVenta = 1000,
                costoUnitario = 500,
                stock = 5,
                stockMinimo = 0,
                categoria = "C",
                fotoUrl = "",
                empresaId = compId
            )
        ).toInt()

        val error = runCatching {
            SaleRepository(db).register(
                RegisterSaleRequest(
                    paymentType = "EFECTIVO",
                    total = -1000,
                    subtotal = 1000,
                    discount = 0,
                    tax = 0,
                    clientId = null,
                    credentialId = null,
                    pin = null,
                    userId = userId,
                    companyId = compId,
                    lines = listOf(SaleLineSnapshot(productId, -1, 1000))
                )
            )
        }.exceptionOrNull()

        assertTrue("Debe rechazarse una cantidad negativa", error is SaleRegistrationException.InvalidQuantity)
        assertEquals(5, productoDao.getByIdIncludingInactive(productId, compId)?.stock)

        db.query("SELECT COUNT(*) FROM ventas", null).use {
            it.moveToFirst()
            assertEquals("No debe crearse la venta", 0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM detalle_ventas", null).use {
            it.moveToFirst()
            assertEquals("No deben crearse detalles", 0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM movimientos_inventario", null).use {
            it.moveToFirst()
            assertEquals("No deben crearse movimientos de inventario", 0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM movimientos_caja", null).use {
            it.moveToFirst()
            assertEquals("No deben crearse movimientos de caja", 0, it.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM auditoria", null).use {
            it.moveToFirst()
            assertEquals("No debe crearse auditoría", 0, it.getInt(0))
        }
    }
}
