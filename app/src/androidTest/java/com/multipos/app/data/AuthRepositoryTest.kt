package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.PasswordHasher
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
class AuthRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var authRepository: AuthRepository
    private val testCompanyId = "test-company"

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        authRepository = AuthRepository(database)
        database.empresaDao().insert(Empresa(testCompanyId, "Test Company"))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun successfulLoginUpdatesLastLoginAndResetsFailures() = runBlocking {
        val digest = PasswordHasher.hash("SecurePass123!".toCharArray())
        val user = Usuario(
            nombre = "Admin",
            usuario = "admin",
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            rol = Usuario.ROL_PROPIETARIO,
            empresaId = testCompanyId,
            intentosFallidos = 2
        )
        val userId = database.usuarioDao().insert(user).toInt()

        val pwd = "SecurePass123!".toCharArray()
        val authenticated = authRepository.authenticate("admin", pwd)

        assertNotNull(authenticated)
        assertEquals(userId, authenticated!!.id)

        val updated = database.usuarioDao().getById(userId)
        assertNotNull(updated)
        assertEquals(0, updated!!.intentosFallidos)
        assertNull(updated.bloqueadoHasta)
        assertNotNull(updated.ultimoLogin)
        assertTrue(updated.ultimoLogin!! > 0L)

        // Verifica que el CharArray fue limpiado en memoria
        assertTrue(pwd.all { it == '\u0000' })
    }

    @Test
    fun failedLoginIncrementsFailures() = runBlocking {
        val digest = PasswordHasher.hash("CorrectPass1!".toCharArray())
        val user = Usuario(
            nombre = "Cajero",
            usuario = "cajero1",
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            rol = Usuario.ROL_CAJERO,
            empresaId = testCompanyId,
            intentosFallidos = 0
        )
        val userId = database.usuarioDao().insert(user).toInt()

        val wrongPwd = "WrongPassword".toCharArray()
        val result = authRepository.authenticate("cajero1", wrongPwd)

        assertNull(result)
        val updated = database.usuarioDao().getById(userId)
        assertNotNull(updated)
        assertEquals(1, updated!!.intentosFallidos)
        assertNull(updated.bloqueadoHasta)
        assertTrue(wrongPwd.all { it == '\u0000' })
    }

    @Test
    fun fifthFailedAttemptLocksUserFor15Minutes() = runBlocking {
        val digest = PasswordHasher.hash("ValidPassword1!".toCharArray())
        val user = Usuario(
            nombre = "Vendedor",
            usuario = "vendedor1",
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            rol = Usuario.ROL_VENDEDOR,
            empresaId = testCompanyId,
            intentosFallidos = 4
        )
        val userId = database.usuarioDao().insert(user).toInt()

        val beforeAttempt = System.currentTimeMillis()
        val result = authRepository.authenticate("vendedor1", "BadPassword".toCharArray())

        assertNull(result)
        val updated = database.usuarioDao().getById(userId)
        assertNotNull(updated)
        assertEquals(5, updated!!.intentosFallidos)
        assertNotNull(updated.bloqueadoHasta)
        assertTrue(updated.bloqueadoHasta!! >= beforeAttempt + Usuario.LOGIN_LOCKOUT_DURATION_MS - 2000L)
    }

    @Test
    fun lockedUserIsRejectedEvenWithCorrectPassword() = runBlocking {
        val digest = PasswordHasher.hash("ValidPassword1!".toCharArray())
        val lockUntil = System.currentTimeMillis() + 10 * 60 * 1000L
        val user = Usuario(
            nombre = "Vendedor",
            usuario = "vendedor2",
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            rol = Usuario.ROL_VENDEDOR,
            empresaId = testCompanyId,
            intentosFallidos = 5,
            bloqueadoHasta = lockUntil
        )
        database.usuarioDao().insert(user)

        val result = authRepository.authenticate("vendedor2", "ValidPassword1!".toCharArray())
        assertNull(result)
    }

    @Test
    fun expiredLockAllowsLoginWithCorrectPassword() = runBlocking {
        val digest = PasswordHasher.hash("ValidPassword1!".toCharArray())
        val expiredLock = System.currentTimeMillis() - 1000L // Bloqueo ya vencido
        val user = Usuario(
            nombre = "Cajero",
            usuario = "cajero2",
            passwordHash = digest.hash,
            passwordSalt = digest.salt,
            rol = Usuario.ROL_CAJERO,
            empresaId = testCompanyId,
            intentosFallidos = 5,
            bloqueadoHasta = expiredLock
        )
        val userId = database.usuarioDao().insert(user).toInt()

        val result = authRepository.authenticate("cajero2", "ValidPassword1!".toCharArray())
        assertNotNull(result)
        assertEquals(userId, result!!.id)

        val updated = database.usuarioDao().getById(userId)
        assertNotNull(updated)
        assertEquals(0, updated!!.intentosFallidos)
        assertNull(updated.bloqueadoHasta)
    }

    @Test
    fun legacyPasswordIsUpgradedOnFirstLogin() = runBlocking {
        val legacyUser = Usuario(
            nombre = "Legacy Admin",
            usuario = "legacyadmin",
            password = "OldPlainPassword123",
            passwordHash = null,
            passwordSalt = null,
            rol = Usuario.ROL_PROPIETARIO,
            empresaId = testCompanyId
        )
        val userId = database.usuarioDao().insert(legacyUser).toInt()

        val result = authRepository.authenticate("legacyadmin", "OldPlainPassword123".toCharArray())
        assertNotNull(result)

        val updated = database.usuarioDao().getById(userId)
        assertNotNull(updated)
        assertEquals("", updated!!.password)
        assertNotNull(updated.passwordHash)
        assertNotNull(updated.passwordSalt)
        assertTrue(updated.requiereCambioClave)
        assertTrue(PasswordHasher.verify("OldPlainPassword123".toCharArray(), updated.passwordHash!!, updated.passwordSalt!!))
    }
}
