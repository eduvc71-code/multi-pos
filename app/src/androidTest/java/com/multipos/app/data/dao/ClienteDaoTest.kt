package com.multipos.app.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.Empresa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClienteDaoTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: ClienteDao
    private val compA = "comp-a"
    private val compB = "comp-b"

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addMigrations(com.multipos.app.data.DatabaseProvider.MIGRATION_8_9)
            .build()
        dao = db.clienteDao()
        runBlocking {
            db.empresaDao().insert(Empresa(id = compA, nombre = "A", tipoNegocio = "T", colorPrimarioHex = "#0", activa = true, nit = "", direccion = "", telefono = "", fechaCreacion = 0))
            db.empresaDao().insert(Empresa(id = compB, nombre = "B", tipoNegocio = "T", colorPrimarioHex = "#0", activa = true, nit = "", direccion = "", telefono = "", fechaCreacion = 0))
        }
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun insertDuplicateDocumentInSameCompanyFails() {
        runBlocking {
            dao.insert(Cliente(nombre = "C1", documento = "123", empresaId = compA))
            try {
                dao.insert(Cliente(nombre = "C2", documento = "123", empresaId = compA))
                fail("Should have thrown SQLiteConstraintException")
            } catch (e: SQLiteConstraintException) {
                // Expected
            }
        }
    }

    @Test
    fun insertSameDocumentInDifferentCompaniesSucceeds() = runBlocking {
        dao.insert(Cliente(nombre = "C1", documento = "123", empresaId = compA))
        dao.insert(Cliente(nombre = "C2", documento = "123", empresaId = compB))
        val clientsA = dao.getAll(compA).first()
        val clientsB = dao.getAll(compB).first()
        assertEquals(1, clientsA.size)
        assertEquals(1, clientsB.size)
    }

    @Test
    fun updateToDuplicateDocumentFails() {
        runBlocking {
            dao.insert(Cliente(id = 1, nombre = "C1", documento = "123", empresaId = compA))
            dao.insert(Cliente(id = 2, nombre = "C2", documento = "456", empresaId = compA))
            val clientToUpdate = dao.getById(2, compA)!!
            try {
                dao.update(clientToUpdate.copy(documento = "123"))
                fail("Should have thrown SQLiteConstraintException")
            } catch (e: SQLiteConstraintException) {
                // Expected
                val originalClient = dao.getById(2, compA)
                assertEquals("456", originalClient?.documento)
            }
        }
    }

    @Test
    fun updateWithValidCreditLimitSucceeds() = runBlocking {
        dao.insert(Cliente(id = 1, nombre = "C1", documento = "123", creditoActual = 500, limiteCredito = 1000, empresaId = compA))
        val updatedRows = dao.updateWithCreditCheck(1, compA, "C1-mod", "123-mod", "555", 800, false, Cliente.ESTADO_NO_SOLICITADO, null, null)
        assertEquals(1, updatedRows)
        val updatedClient = dao.getById(1, compA)!!
        assertEquals("C1-mod", updatedClient.nombre)
        assertEquals(800L, updatedClient.limiteCredito)
    }

    @Test
    fun updateWithCreditLimitEqualToBalanceSucceeds() = runBlocking {
        dao.insert(Cliente(id = 1, nombre = "C1", documento = "123", creditoActual = 500, limiteCredito = 1000, empresaId = compA))
        val updatedRows = dao.updateWithCreditCheck(1, compA, "C1-mod", "123-mod", "555", 500, false, Cliente.ESTADO_NO_SOLICITADO, null, null)
        assertEquals(1, updatedRows)
        val updatedClient = dao.getById(1, compA)!!
        assertEquals(500L, updatedClient.limiteCredito)
    }

    @Test
    fun updateWithInvalidCreditLimitFails() = runBlocking {
        dao.insert(Cliente(id = 1, nombre = "C1", documento = "123", creditoActual = 500, limiteCredito = 1000, empresaId = compA))
        val updatedRows = dao.updateWithCreditCheck(1, compA, "C1-mod", "123-mod", "555", 400, false, Cliente.ESTADO_NO_SOLICITADO, null, null)
        assertEquals(0, updatedRows)
        val client = dao.getById(1, compA)!!
        assertEquals("C1", client.nombre)
        assertEquals(1000L, client.limiteCredito)
    }

    @Test
    fun updateNonExistentClientReturnsZero() = runBlocking {
        val updatedRows = dao.updateWithCreditCheck(99, compA, "C-ghost", "doc-ghost", "", 100, false, "", null, null)
        assertEquals(0, updatedRows)
    }

    @Test
    fun archiveHidesClientAndDisablesCreditButPreservesRow() = runBlocking {
        val clientId = dao.insert(
            Cliente(
                nombre = "C1",
                documento = "123",
                limiteCredito = 1000,
                creditoHabilitado = true,
                estadoCredito = Cliente.ESTADO_ACTIVO,
                empresaId = compA
            )
        ).toInt()

        assertEquals(1, dao.archive(clientId, compA))
        assertEquals(0, dao.getAll(compA).first().size)
        assertEquals(null, dao.getById(clientId, compA))

        val archived = dao.getByIdIncludingInactive(clientId, compA)
        assertNotNull(archived)
        assertFalse(archived!!.activo)
        assertFalse(archived.creditoHabilitado)
        assertEquals(Cliente.ESTADO_CANCELADO, archived.estadoCredito)
    }

    @Test
    fun archiveClientWithBalanceIsRejectedWithoutChanges() = runBlocking {
        val clientId = dao.insert(
            Cliente(
                nombre = "C1",
                documento = "123",
                limiteCredito = 1000,
                creditoActual = 500,
                creditoHabilitado = true,
                estadoCredito = Cliente.ESTADO_ACTIVO,
                empresaId = compA
            )
        ).toInt()

        assertEquals(0, dao.archive(clientId, compA))
        val stored = dao.getById(clientId, compA)
        assertNotNull(stored)
        assertTrue(stored!!.activo)
        assertTrue(stored.creditoHabilitado)
        assertEquals(Cliente.ESTADO_ACTIVO, stored.estadoCredito)
    }
}
