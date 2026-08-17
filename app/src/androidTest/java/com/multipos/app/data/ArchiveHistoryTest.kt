package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Producto
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveHistoryTest {
    private lateinit var db: AppDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun closeDatabase() = db.close()

    @Test
    fun archivingProductAndClientPreservesSaleDetailsAndPayments() = runBlocking {
        val companyId = "company-a"
        db.empresaDao().insert(com.multipos.app.data.entities.Empresa(companyId, "Empresa A"))
        val productId = db.productoDao().insert(
            Producto(
                nombre = "Producto histórico",
                codigo = "SKU-HISTORY",
                precioVenta = 1000,
                costoUnitario = 500,
                stock = 10,
                empresaId = companyId
            )
        ).toInt()
        val clientId = db.clienteDao().insert(
            Cliente(nombre = "Cliente histórico", documento = "DOC-HISTORY", empresaId = companyId)
        ).toInt()
        val userId = db.usuarioDao().insert(
            com.multipos.app.data.entities.Usuario(nombre = "Operador", usuario = "operador", rol = com.multipos.app.data.entities.Usuario.ROL_ADMINISTRADOR, empresaId = companyId)
        ).toInt()
        db.usuarioEmpresaDao().insert(com.multipos.app.data.entities.UsuarioEmpresa(userId, companyId, com.multipos.app.data.entities.Usuario.ROL_ADMINISTRADOR))
        val saleId = db.ventaDao().insert(
            Venta(
                tipoPago = "EFECTIVO",
                total = 1000,
                idCliente = clientId,
                idUsuario = userId,
                empresaId = companyId
            )
        ).toInt()
        db.ventaDao().insertDetalles(
            listOf(
                DetalleVenta(
                    idVenta = saleId,
                    idProducto = productId,
                    cantidad = 1,
                    precioUnitario = 1000,
                    subtotal = 1000,
                    empresaId = companyId
                )
            )
        )
        db.abonoDao().insert(
            Abono(empresaId = companyId, idCliente = clientId, idVenta = saleId, usuarioId = userId, monto = 500, medioPago = Abono.MEDIO_EFECTIVO, nota = "")
        )

        assertEquals(1, db.productoDao().archive(productId, companyId))
        assertEquals(1, db.clienteDao().archive(clientId, companyId))

        assertRowCount("ventas", 1)
        assertRowCount("detalle_ventas", 1)
        assertRowCount("abonos", 1)
        assertEquals(productId, queryInt("SELECT idProducto FROM detalle_ventas WHERE idVenta = $saleId"))
        assertEquals(clientId, queryInt("SELECT idCliente FROM ventas WHERE id = $saleId"))
        assertEquals(clientId, queryInt("SELECT idCliente FROM abonos WHERE idVenta = $saleId"))
    }

    private fun assertRowCount(table: String, expected: Int) {
        assertEquals(expected, queryInt("SELECT COUNT(*) FROM $table"))
    }

    private fun queryInt(sql: String): Int = db.openHelper.readableDatabase.query(sql).use {
        it.moveToFirst()
        it.getInt(0)
    }
}
