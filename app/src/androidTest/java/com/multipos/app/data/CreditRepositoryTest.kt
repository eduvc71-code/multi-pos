package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Abono
import com.multipos.app.data.entities.CajaSesion
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.data.entities.MovimientoCredito
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
class CreditRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var repository: CreditRepository
    private var adminId: Int = 0
    private var cashierId: Int = 0
    private var clientId: Int = 0

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.empresaDao().insert(Empresa(COMPANY, "Empresa"))
        adminId = createUser("admin", Usuario.ROL_ADMINISTRADOR)
        cashierId = createUser("cashier", Usuario.ROL_CAJERO)
        clientId = database.clienteDao().insert(
            Cliente(
                nombre = "Cliente de crédito",
                documento = "DOC-CREDIT",
                limiteCredito = 10_000,
                creditoActual = 3_000,
                creditoHabilitado = true,
                estadoCredito = Cliente.ESTADO_ACTIVO,
                empresaId = COMPANY
            )
        ).toInt()
        repository = CreditRepository(database)
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun registerAbonoReducesBalanceAndWritesLedgerCashAndAudit() = runBlocking {
        openCashSession()
        repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 1_000,
                medioPago = Abono.MEDIO_EFECTIVO,
                nota = "Abono de crédito"
            )
        )

        assertEquals(2_000L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
        database.abonoDao().getByClient(COMPANY, clientId).let {
            assertEquals(1, it.size)
            assertEquals(1_000L, it[0].monto)
            assertEquals(Abono.MEDIO_EFECTIVO, it[0].medioPago)
            assertEquals(adminId, it[0].usuarioId)
        }
        database.movimientoCreditoDao().getByClient(COMPANY, clientId).let {
            assertEquals(1, it.size)
            assertEquals(MovimientoCredito.TIPO_ABONO, it[0].tipo)
            assertEquals(-1_000L, it[0].importeFirmado)
            assertEquals(2_000L, it[0].saldoPosterior)
        }
        val cashMovement = database.movimientoCajaDao()
            .getBySession(sessionId(COMPANY), COMPANY)
            .first { it.tipo == MovimientoCaja.TIPO_INGRESO_ABONO }
        assertEquals(1_000L, cashMovement.monto)
        val audit = database.auditoriaDao()
            .getByCompanyAndDateRange(COMPANY, 0, Long.MAX_VALUE)
            .first { it.accion == "ABONO" }
        assertEquals("abono", audit.entidad)
    }

    @Test
    fun registerAbonoReturnsTransactionalBalances() = runBlocking {
        openCashSession()
        val result = repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 1_000,
                medioPago = Abono.MEDIO_EFECTIVO,
                externalPaymentConfirmed = true
            )
        )
        assertEquals(3_000L, result.saldoAnterior)
        assertEquals(2_000L, result.saldoNuevo)
        assertEquals(2_000L, result.saldoAnterior - 1_000L)
        val ledger = database.movimientoCreditoDao().getByClient(COMPANY, clientId)[0]
        assertEquals(result.saldoNuevo, ledger.saldoPosterior)
    }

    @Test
    fun registerAbonoRequiresCashSessionForEffectivePayment() = runBlocking {
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = Abono.MEDIO_EFECTIVO,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull()
        assertTrue(error is CreditException.NoActiveCashSession)
    }

    @Test
    fun registerAbonoCannotExceedBalanceAndIsAtomic() = runBlocking {
        openCashSession()
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 5_000,
                    medioPago = Abono.MEDIO_EFECTIVO,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull()
                assertTrue(error is CreditException.CreditDebtNotEnough)
        assertEquals(3_000L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
        assertEquals(0, database.abonoDao().getByClient(COMPANY, clientId).size)
        assertEquals(0, database.movimientoCreditoDao().getByClient(COMPANY, clientId).size)
        assertEquals(0, database.movimientoCajaDao().getBySession(sessionId(COMPANY), COMPANY).size)
    }

    @Test
    fun cashierCannotRegisterAbono() = runBlocking {
        openCashSession()
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = cashierId,
                    monto = 500,
                    medioPago = Abono.MEDIO_EFECTIVO,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull()
        assertTrue(error is CreditException.NotAuthorized)
    }

    @Test
    fun nonCashPaymentRequiresExternalConfirmationAndNoCashSession() = runBlocking {
        val rejected = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = Abono.MEDIO_TARJETA,
                    externalPaymentConfirmed = false
                )
            )
        }.exceptionOrNull()
        assertTrue(rejected is CreditException.ExternalPaymentNotConfirmed)

        val accepted = repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 500,
                medioPago = Abono.MEDIO_TRANSFERENCIA,
                externalPaymentConfirmed = true
            )
        )
        assertTrue(accepted.abonoId > 0)
        assertEquals(2_500L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
        assertEquals(0, database.movimientoCajaDao().getBySession(0L, COMPANY).size)
    }

    @Test
    fun rejectsInvalidMedioPago() = runBlocking {
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = "CRIPTO"
                )
            )
        }.exceptionOrNull()
        assertTrue(error is CreditException.InvalidMedioPago)
    }

    @Test
    fun rejectsOversizedNote() = runBlocking {
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = Abono.MEDIO_TARJETA,
                    nota = "X".repeat(301),
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull()
        assertTrue(error is CreditException.InvalidNote)
    }

    @Test
    fun inactiveClientCannotRegisterAbono() = runBlocking {
        val inactiveId = database.clienteDao().insert(
            Cliente(
                nombre = "Inactivo",
                documento = "DOC-INACTIVE",
                limiteCredito = 10_000,
                creditoActual = 1_000,
                creditoHabilitado = true,
                estadoCredito = Cliente.ESTADO_ACTIVO,
                activo = false,
                empresaId = COMPANY
            )
        ).toInt()
        val error = runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = inactiveId,
                    userId = adminId,
                    monto = 100,
                    medioPago = Abono.MEDIO_TARJETA,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull()
        assertTrue(error is CreditException.ClientNotAllowed)
    }

    @Test
    fun suspendedClientCanRegisterAbono() = runBlocking {
        openCashSession()
        database.clienteDao().update(
            database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.copy(estadoCredito = Cliente.ESTADO_SUSPENDIDO)
        )
        val result = repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 500,
                medioPago = Abono.MEDIO_EFECTIVO,
                externalPaymentConfirmed = true
            )
        )
        assertTrue(result.abonoId > 0)
        assertEquals(3_000L, result.saldoAnterior)
        assertEquals(2_500L, result.saldoNuevo)
        assertEquals(2_500L, database.clienteDao().getByIdIncludingInactive(clientId, COMPANY)!!.creditoActual)
    }

    @Test
    fun closedOrForeignCashSessionRejected() = runBlocking {
        openCashSession()
        val closed = database.cajaSesionDao().insert(
            CajaSesion(
                empresaId = COMPANY,
                abiertaPorUsuarioId = adminId,
                fechaApertura = System.currentTimeMillis(),
                montoApertura = 0,
                estado = CajaSesion.ESTADO_CERRADA,
                notaCierre = ""
            )
        )
        val foreign = database.cajaSesionDao().insert(
            CajaSesion(
                empresaId = "other-company",
                abiertaPorUsuarioId = adminId,
                fechaApertura = System.currentTimeMillis(),
                montoApertura = 0,
                estado = CajaSesion.ESTADO_ABIERTA,
                notaCierre = ""
            )
        )
        assertTrue(runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = Abono.MEDIO_EFECTIVO,
                    cajaSesionId = closed,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull() is CreditException.InvalidCashSession)
        assertTrue(runCatching {
            repository.registerAbono(
                RegisterAbonoRequest(
                    companyId = COMPANY,
                    clientId = clientId,
                    userId = adminId,
                    monto = 500,
                    medioPago = Abono.MEDIO_EFECTIVO,
                    cajaSesionId = foreign,
                    externalPaymentConfirmed = true
                )
            )
        }.exceptionOrNull() is CreditException.InvalidCashSession)
    }

    @Test
    fun estadoDeCuentaAllowsAnyCompanyMember() = runBlocking {
        openCashSession()
        repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 1_000,
                medioPago = Abono.MEDIO_EFECTIVO,
                externalPaymentConfirmed = true
            )
        )
        val rows = repository.estadoDeCuenta(COMPANY, clientId, cashierId)
        assertEquals(1, rows.size)
    }

    @Test
    fun estadoDeCuentaRejectsNonMemberAndFiltersByRange() = runBlocking {
        openCashSession()
        val outsiderId = database.usuarioDao().insert(
            Usuario(nombre = "fora", usuario = "outsider", rol = Usuario.ROL_CAJERO, empresaId = "other-company")
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(outsiderId, "other-company", Usuario.ROL_CAJERO))

        assertTrue(runCatching { repository.estadoDeCuenta(COMPANY, clientId, outsiderId) }
            .exceptionOrNull() is CreditException.NotAuthorized)

        repository.registerAbono(
            RegisterAbonoRequest(
                companyId = COMPANY,
                clientId = clientId,
                userId = adminId,
                monto = 1_000,
                medioPago = Abono.MEDIO_EFECTIVO,
                externalPaymentConfirmed = true
            )
        )
        val now = System.currentTimeMillis()
        assertEquals(1, repository.estadoDeCuenta(COMPANY, clientId, adminId, now - 3_600_000, now + 3_600_000).size)
        assertEquals(0, repository.estadoDeCuenta(COMPANY, clientId, adminId, now + 3_600_000, now + 7_200_000).size)
    }

    @Test
    fun estadoDeCuentaDateFilterIsInclusiveFromExclusiveTo() = runBlocking {
        openCashSession()
        val inserted = database.movimientoCreditoDao().insert(
            MovimientoCredito(
                empresaId = COMPANY,
                clienteId = clientId,
                usuarioId = adminId,
                tipo = MovimientoCredito.TIPO_ABONO,
                importeFirmado = -100,
                saldoPosterior = 2_900,
                ventaId = null,
                abonoId = null,
                devolucionId = null,
                fecha = 1_000_000L,
                nota = "boundary"
            )
        )
        assertTrue(inserted > 0)
        val atStart = database.movimientoCreditoDao().getByClientBetween(COMPANY, clientId, 1_000_000L, 1_000_001L)
        assertEquals(1, atStart.size)
        val atStartOpen = database.movimientoCreditoDao().getByClientBetween(COMPANY, clientId, 1_000_000L, 1_000_005L)
        assertEquals(1, atStartOpen.size)
        val exclusiveTo = database.movimientoCreditoDao().getByClientBetween(COMPANY, clientId, 999_999L, 1_000_000L)
        assertEquals(0, exclusiveTo.size)
    }

    private suspend fun openCashSession() {
        database.cajaSesionDao().insert(
            CajaSesion(
                empresaId = COMPANY,
                abiertaPorUsuarioId = adminId,
                fechaApertura = System.currentTimeMillis(),
                montoApertura = 0,
                estado = CajaSesion.ESTADO_ABIERTA,
                notaCierre = ""
            )
        )
    }

    private suspend fun sessionId(companyId: String): Long =
        database.cajaSesionDao().getActiveSessionForCompany(companyId)!!.id

    private suspend fun createUser(username: String, role: String): Int {
        val id = database.usuarioDao().insert(
            Usuario(nombre = username, usuario = username, rol = role, empresaId = COMPANY)
        ).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(id, COMPANY, role))
        return id
    }

    private companion object {
        const val COMPANY = "credit-company"
    }
}