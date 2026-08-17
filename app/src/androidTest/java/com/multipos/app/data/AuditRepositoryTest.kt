package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuditRepositoryTest {
    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private val companyA = "company-a"
    private val companyB = "company-b"

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.empresaDao().insert(Empresa(companyA, "Empresa A"))
        database.empresaDao().insert(Empresa(companyB, "Empresa B"))
    }

    @After
    fun tearDown() {
        database.close()
        UserSessionStore.clear(context)
    }

    @Test
    fun ownerCanQueryAuditLogsForActiveCompany() = runBlocking {
        val owner = Usuario(nombre = "Owner", usuario = "owner", rol = Usuario.ROL_PROPIETARIO, empresaId = companyA)
        val ownerId = database.usuarioDao().insert(owner).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(ownerId, companyA, Usuario.ROL_PROPIETARIO, true))

        database.auditoriaDao().insert(
            Auditoria(
                empresaId = companyA,
                usuarioId = ownerId,
                accion = Auditoria.ACCION_LOGIN_OK,
                entidad = "usuario",
                entidadId = ownerId.toString(),
                detalle = "login correcto",
                fecha = System.currentTimeMillis()
            )
        )

        UserSessionStore.set(context, owner.copy(id = ownerId))
        ActiveCompanyStore.set(context, companyA)

        val repo = AuditRepository(database, context)
        val logs = repo.getForActiveCompany().first()

        assertEquals(1, logs.size)
        assertEquals(Auditoria.ACCION_LOGIN_OK, logs[0].accion)
        assertEquals(companyA, logs[0].empresaId)
    }

    @Test
    fun nonOwnerRoleIsRejectedByRepository() = runBlocking {
        val admin = Usuario(nombre = "Admin", usuario = "admin", rol = Usuario.ROL_ADMINISTRADOR, empresaId = companyA)
        val adminId = database.usuarioDao().insert(admin).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(adminId, companyA, Usuario.ROL_ADMINISTRADOR, true))

        UserSessionStore.set(context, admin.copy(id = adminId))
        ActiveCompanyStore.set(context, companyA)

        val repo = AuditRepository(database, context)

        var caught = false
        try {
            repo.getForActiveCompany()
        } catch (e: IllegalArgumentException) {
            caught = true
        }
        org.junit.Assert.assertTrue("Debe lanzar IllegalArgumentException para usuarios no propietarios", caught)
    }

    @Test
    fun auditLogsAreIsolatedByCompany() = runBlocking {
        val owner = Usuario(nombre = "Owner", usuario = "owner", rol = Usuario.ROL_PROPIETARIO, empresaId = companyA)
        val ownerId = database.usuarioDao().insert(owner).toInt()
        database.usuarioEmpresaDao().insert(UsuarioEmpresa(ownerId, companyA, Usuario.ROL_PROPIETARIO, true))

        database.auditoriaDao().insert(
            Auditoria(empresaId = companyA, usuarioId = ownerId, accion = "VENTA", entidad = "venta", detalle = "A", fecha = 1000L)
        )
        database.auditoriaDao().insert(
            Auditoria(empresaId = companyB, usuarioId = ownerId, accion = "VENTA", entidad = "venta", detalle = "B", fecha = 2000L)
        )

        UserSessionStore.set(context, owner.copy(id = ownerId))
        ActiveCompanyStore.set(context, companyA)

        val repo = AuditRepository(database, context)
        val logs = repo.getForActiveCompany().first()

        assertEquals(1, logs.size)
        assertEquals(companyA, logs[0].empresaId)
    }
}
