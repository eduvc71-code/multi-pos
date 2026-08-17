package com.multipos.app.data.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.AppDatabase
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanyMembershipDaoTest {
    private lateinit var database: AppDatabase

    @Before
    fun createDatabase() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        database.empresaDao().insert(Empresa(COMPANY_A, "Empresa A"))
        database.empresaDao().insert(Empresa(COMPANY_B, "Empresa B"))
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun employeeListUsesRoleAndStateFromCurrentCompanyMembership() = runBlocking {
        val userId = createUserWithTwoMemberships()

        val companyBMember = database.usuarioDao().getByCompany(COMPANY_B).first().single()

        assertEquals(userId, companyBMember.id)
        assertEquals(Usuario.ROL_CAJERO, companyBMember.rol)
        assertTrue(companyBMember.activo)
    }

    @Test
    fun deactivatingMembershipOnlyAffectsSelectedCompany() = runBlocking {
        val userId = createUserWithTwoMemberships()

        assertEquals(1, database.usuarioEmpresaDao().setActive(userId, COMPANY_B, false))

        val companyBMember = database.usuarioDao().getByCompany(COMPANY_B).first().single()
        assertFalse(companyBMember.activo)
        assertNull(database.usuarioEmpresaDao().getActiveMembership(userId, COMPANY_B))
        assertNotNull(database.usuarioEmpresaDao().getActiveMembership(userId, COMPANY_A))
        assertNotNull(database.usuarioDao().getById(userId))
    }

    private suspend fun createUserWithTwoMemberships(): Int {
        val userId = database.usuarioDao().insert(
            Usuario(
                nombre = "Usuario",
                usuario = "usuario.prueba",
                rol = Usuario.ROL_PROPIETARIO,
                empresaId = COMPANY_A
            )
        ).toInt()
        database.usuarioEmpresaDao().insert(
            UsuarioEmpresa(userId, COMPANY_A, Usuario.ROL_PROPIETARIO)
        )
        database.usuarioEmpresaDao().insert(
            UsuarioEmpresa(userId, COMPANY_B, Usuario.ROL_CAJERO)
        )
        return userId
    }

    private companion object {
        const val COMPANY_A = "company-a"
        const val COMPANY_B = "company-b"
    }
}
