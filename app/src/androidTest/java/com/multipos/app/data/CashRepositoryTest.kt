package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CashRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CashRepository
    private var ownerId: Int = 0
    private var adminId: Int = 0
    private var cashierId: Int = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.openHelper.writableDatabase.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_caja_sesiones_una_abierta_por_empresa " +
                "ON caja_sesiones(empresaId) WHERE estado = 'ABIERTA'"
        )
        database.empresaDao().insert(Empresa(COMPANY_A, "Empresa A"))
        database.empresaDao().insert(Empresa(COMPANY_B, "Empresa B"))
        ownerId = createUser("owner", Usuario.ROL_PROPIETARIO)
        adminId = createUser("admin", Usuario.ROL_ADMINISTRADOR)
        cashierId = createUser("cashier", Usuario.ROL_CAJERO)
        repository = CashRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun concurrentOpeningLeavesExactlyOneOpenSessionPerCompany() = runBlocking {
        val results = supervisorScope {
            listOf(ownerId, adminId).map { userId ->
                async(Dispatchers.IO) {
                    runCatching { repository.openSession(COMPANY_A, userId, 0) }
                }
            }.awaitAll()
        }

        assertEquals(1, results.count { it.isSuccess })
        assertEquals(1, results.count { it.exceptionOrNull() is CashException.SessionAlreadyOpen })
        database.query(
            "SELECT COUNT(*) FROM caja_sesiones WHERE empresaId = ? AND estado = 'ABIERTA'",
            arrayOf(COMPANY_A)
        ).use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }

        repository.openSession(COMPANY_B, ownerId, 0)
        assertTrue(repository.getActiveSession(COMPANY_B) != null)
    }

    @Test
    fun expectedCashCountsOpeningOnceAndPersistsDifference() = runBlocking {
        val session = repository.openSession(COMPANY_A, ownerId, 10_000)
        repository.registerManualMovement(
            COMPANY_A,
            session.id,
            ownerId,
            MovimientoCaja.TIPO_INGRESO_MANUAL,
            2_000,
            "Ingreso auxiliar"
        )
        repository.registerManualMovement(
            COMPANY_A,
            session.id,
            ownerId,
            MovimientoCaja.TIPO_EGRESO_MANUAL,
            1_500,
            "Retiro operativo"
        )

        val balance = repository.getSessionWithBalance(COMPANY_A, session.id)!!
        assertEquals(2_000L, balance.ingresos)
        assertEquals(1_500L, balance.egresos)
        assertEquals(10_500L, balance.expected)

        val closed = repository.closeSession(COMPANY_A, session.id, ownerId, 10_000, "Faltante de caja")
        assertEquals(-500L, closed.difference)
        assertEquals(-500L, closed.session.diferenciaCierre)
    }

    @Test
    fun cashierCannotCreateManualMovementOrCloseAnotherCashiersSession() = runBlocking {
        val session = repository.openSession(COMPANY_A, adminId, 0)

        val movementError = runCatching {
            repository.registerManualMovement(
                COMPANY_A,
                session.id,
                cashierId,
                MovimientoCaja.TIPO_INGRESO_MANUAL,
                100,
                "Ingreso no permitido"
            )
        }.exceptionOrNull()
        assertTrue(movementError is CashException.NotAuthorized)

        val closeError = runCatching {
            repository.closeSession(COMPANY_A, session.id, cashierId, 0, "")
        }.exceptionOrNull()
        assertTrue(closeError is CashException.NotAuthorized)
    }

    private suspend fun createUser(username: String, role: String): Int {
        val userId = database.usuarioDao().insert(
            Usuario(nombre = username, usuario = username, rol = role, empresaId = COMPANY_A)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, COMPANY_A, role))
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(userId, COMPANY_B, role))
        return userId
    }

    private companion object {
        const val COMPANY_A = "cash-company-a"
        const val COMPANY_B = "cash-company-b"
    }
}
