package com.multipos.app.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AbonoDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: AbonoDao
    private val compA = "comp-a"
    private var client1: Int = 0
    private var client2: Int = 0
    private var sale1: Int = 0
    private var sale2: Int = 0
    private var user: Int = 0

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = db.abonoDao()
        runBlocking {
            db.empresaDao().insert(Empresa(id = compA, nombre = "Comp A"))
            user = db.usuarioDao().insert(
                Usuario(nombre = "Operador", usuario = "operador", rol = Usuario.ROL_ADMINISTRADOR, empresaId = compA)
            ).toInt()
            db.usuarioEmpresaDao().insert(UsuarioEmpresa(user, compA, Usuario.ROL_ADMINISTRADOR))
            client1 = db.clienteDao().insert(Cliente(nombre = "Cliente 1", documento = "DOC-1", empresaId = compA)).toInt()
            client2 = db.clienteDao().insert(Cliente(nombre = "Cliente 2", documento = "DOC-2", empresaId = compA)).toInt()
            sale1 = db.ventaDao().insert(Venta(tipoPago = "CREDITO", total = 2000, idCliente = client1, idUsuario = user, empresaId = compA)).toInt()
            sale2 = db.ventaDao().insert(Venta(tipoPago = "CREDITO", total = 1500, idCliente = client2, idUsuario = user, empresaId = compA)).toInt()
        }
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun totalForClientSumCorrectly() = runBlocking {
        dao.insert(Abono(id = 1, empresaId = compA, idCliente = client1, idVenta = sale1, usuarioId = user, monto = 500, medioPago = Abono.MEDIO_EFECTIVO, fecha = 0, nota = ""))
        dao.insert(Abono(id = 2, empresaId = compA, idCliente = client1, idVenta = sale1, usuarioId = user, monto = 300, medioPago = Abono.MEDIO_EFECTIVO, fecha = 0, nota = ""))
        dao.insert(Abono(id = 3, empresaId = compA, idCliente = client2, idVenta = sale2, usuarioId = user, monto = 1000, medioPago = Abono.MEDIO_EFECTIVO, fecha = 0, nota = ""))

        assertEquals(800L, dao.totalForClient(client1, compA))
        assertEquals(1000L, dao.totalForClient(client2, compA))
    }
}
