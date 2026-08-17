package com.multipos.app.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VentaDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: VentaDao
    private val compA = "comp-a"
    private val compB = "comp-b"

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.ventaDao()
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun totalSinceCalculatesCorrectly() = runBlocking {
        val now = 1000L
        dao.insert(Venta(id = 1, fecha = now - 100, tipoPago = "E", total = 1000, subtotal = 1000, descuento = 0, impuesto = 0, idUsuario = 1, estado = "C", empresaId = compA))
        dao.insert(Venta(id = 2, fecha = now + 100, tipoPago = "E", total = 2000, subtotal = 2000, descuento = 0, impuesto = 0, idUsuario = 1, estado = "C", empresaId = compA))
        dao.insert(Venta(id = 3, fecha = now + 200, tipoPago = "E", total = 5000, subtotal = 5000, descuento = 0, impuesto = 0, idUsuario = 1, estado = "C", empresaId = compB))

        val totalA = dao.totalSince(now, compA)
        assertEquals(2000L, totalA)

        val totalB = dao.totalSince(now, compB)
        assertEquals(5000L, totalB)
    }

    @Test
    fun getAllFiltersByCompany() = runBlocking {
        dao.insert(Venta(id = 1, fecha = 0, tipoPago = "E", total = 10, subtotal = 10, descuento = 0, impuesto = 0, idUsuario = 1, estado = "C", empresaId = compA))
        dao.insert(Venta(id = 2, fecha = 0, tipoPago = "E", total = 20, subtotal = 20, descuento = 0, impuesto = 0, idUsuario = 1, estado = "C", empresaId = compB))

        assertEquals(1, dao.getAll(compA).first().size)
        assertEquals(1, dao.getAll(compB).first().size)
    }
}
