package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.MovimientoInventario
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryMovementRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: InventoryMovementRepository
    private var ownerId: Int = 0
    private var cashierId: Int = 0
    private var productId: Int = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.empresaDao().insert(Empresa(COMPANY, "Empresa A"))
        ownerId = createUser("owner", Usuario.ROL_PROPIETARIO)
        createUser("admin", Usuario.ROL_ADMINISTRADOR)
        cashierId = createUser("cashier", Usuario.ROL_CAJERO)
        productId = database.productoDao().insert(
            Producto(
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
        repository = InventoryMovementRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun manualEntryIncreasesStockAndWritesAudit() = runBlocking {
        repository.registerMovement(
            InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_ENTRADA_MANUAL, 3, "Compra a proveedor")
        )

        assertEquals(13, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
        val movement = database.movimientoInventarioDao().getByProduct(COMPANY, productId).single()
        assertEquals(MovimientoInventario.TIPO_ENTRADA_MANUAL, movement.tipo)
        assertEquals(3, movement.cantidadFirmada)
        assertEquals(10, movement.stockAnterior)
        assertEquals(13, movement.stockPosterior)
        val audit = database.auditoriaDao().getByCompanyAndDateRange(COMPANY, 0, Long.MAX_VALUE)
        assertTrue(audit.any { it.accion == com.multipos.app.data.entities.Auditoria.ACCION_MOVIMIENTO_INVENTARIO })
    }

    @Test
    fun manualExitDecreasesStock() = runBlocking {
        repository.registerMovement(
            InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_SALIDA_MANUAL, 4, "Salida por merma")
        )

        assertEquals(6, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
        val movement = database.movimientoInventarioDao().getByProduct(COMPANY, productId).single()
        assertEquals(-4, movement.cantidadFirmada)
    }

    @Test
    fun exitBeyondStockFails() = runBlocking {
        val error = runCatching {
            repository.registerMovement(
                InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_SALIDA_MANUAL, 11, "Salida excesiva")
            )
        }.exceptionOrNull()

        assertTrue(error is InventoryMovementException.InsufficientStock)
        assertEquals(10, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
    }

    @Test
    fun adjustmentSetsTargetStockAndDerivesSignedQuantity() = runBlocking {
        repository.registerMovement(
            InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_AJUSTE, 7, "Ajuste a stock objetivo")
        )

        assertEquals(7, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
        val movement = database.movimientoInventarioDao().getByProduct(COMPANY, productId).single()
        assertEquals(MovimientoInventario.TIPO_AJUSTE, movement.tipo)
        assertEquals(-3, movement.cantidadFirmada)
        assertEquals(10, movement.stockAnterior)
        assertEquals(7, movement.stockPosterior)
    }

    @Test
    fun adjustmentRejectsTargetEqualToCurrentStock() = runBlocking {
        val error = runCatching {
            repository.registerMovement(
                InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_AJUSTE, 10, "Sin cambio de stock")
            )
        }.exceptionOrNull()

        assertTrue(error is InventoryMovementException.InvalidQuantity)
        assertEquals(10, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
    }

    @Test
    fun adjustmentCannotGoBelowZero() = runBlocking {
        val error = runCatching {
            repository.registerMovement(
                InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_AJUSTE, -1, "Stock objetivo inválido")
            )
        }.exceptionOrNull()

        assertTrue(error is InventoryMovementException.InsufficientStock)
        assertEquals(10, database.productoDao().getByIdIncludingInactive(productId, COMPANY)!!.stock)
    }

    @Test
    fun cashierCannotRegisterMovements() = runBlocking {
        val error = runCatching {
            repository.registerMovement(
                InventoryMovementRequest(COMPANY, productId, cashierId, MovimientoInventario.TIPO_ENTRADA_MANUAL, 1, "Intento sin permiso")
            )
        }.exceptionOrNull()

        assertTrue(error is InventoryMovementException.NotAuthorized)
    }

    @Test
    fun motivoMustBeBetween5And300() = runBlocking {
        val error = runCatching {
            repository.registerMovement(
                InventoryMovementRequest(COMPANY, productId, ownerId, MovimientoInventario.TIPO_ENTRADA_MANUAL, 1, "corto")
            )
        }.exceptionOrNull()

        assertTrue(error is InventoryMovementException.InvalidReason)
    }

    private suspend fun createUser(username: String, role: String): Int {
        val userId = database.usuarioDao().insert(
            Usuario(nombre = username, usuario = username, rol = role, empresaId = COMPANY)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, COMPANY, role))
        return userId
    }

    private companion object {
        const val COMPANY = "movements-company"
    }
}
