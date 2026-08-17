package com.multipos.app.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun migrate4To5() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, activa) VALUES ('c1', 'N', 'T', '#0', 1)")
            execSQL("INSERT INTO usuarios (id, nombre, usuario, password, rol, activo) VALUES (1, 'Admin', 'admin', 'pass', 'ADMIN', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, DatabaseProvider.MIGRATION_4_5)

        // Validar transformación ADMIN -> PROPIETARIO
        db.query("SELECT rol FROM usuarios WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("PROPIETARIO", it.getString(0))
        }
        // Validar nueva tabla usuario_empresas poblada
        db.query("SELECT COUNT(*) FROM usuario_empresas WHERE usuarioId = 1 AND empresaId = 'default-company'").use {
            it.moveToFirst()
            assertEquals(1, it.getInt(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate5To6() {
        helper.createDatabase(TEST_DB, 5).apply {
            execSQL("INSERT INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, activa, nit, direccion, telefono, fechaCreacion) VALUES ('c1', 'N', 'T', '#0', 1, '', '', '', 0)")
            execSQL("INSERT INTO usuarios (id, nombre, usuario, password, rol, activo, empresaId, requiereCambioClave, fechaCreacion) VALUES (1, 'U', 'u', 'p', 'ROL', 1, 'c1', 0, 0)")
            execSQL("INSERT INTO productos (id, nombre, codigo, precioVenta, costoUnitario, stock, stockMinimo, categoria, fotoUrl, empresaId) VALUES (1, 'P', 'C', 10.0, 5.0, 10, 1, 'C', '', 'c1')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 6, true, DatabaseProvider.MIGRATION_5_6)

        // Validar nuevas columnas en productos
        db.query("SELECT codigoBarras FROM productos WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(null, it.getString(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate6To7() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL("INSERT INTO clientes (id, nombre, documento, telefono, direccion, limiteCredito, creditoActual, empresaId) VALUES (1, 'J', '1', '', '', 100.0, 0.0, 'c1')")
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, DatabaseProvider.MIGRATION_6_7)

        // Validar nuevas columnas en clientes
        db.query("SELECT creditoHabilitado, estadoCredito FROM clientes WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertEquals("PENDIENTE", it.getString(1))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate7To8() {
        helper.createDatabase(TEST_DB, 7).apply {
            val values = ContentValues().apply {
                put("id", 1)
                put("nombre", "P")
                put("codigo", "C")
                put("precioVenta", 10.55)
                put("costoUnitario", 5.0)
                put("stock", 10)
                put("stockMinimo", 1)
                put("categoria", "C")
                put("fotoUrl", "")
                put("empresaId", "c1")
            }
            insert("productos", SQLiteDatabase.CONFLICT_REPLACE, values)
            close()
        }

        val db = helper.runMigrationsAndValidate(TEST_DB, 8, true, DatabaseProvider.MIGRATION_7_8)

        // Validar conversión monetaria REAL -> INTEGER (* 100)
        db.query("SELECT precioVenta FROM productos WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1055L, it.getLong(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateChain4To8() {
        helper.createDatabase(TEST_DB, 4).apply {
            execSQL("INSERT INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, activa) VALUES ('c1', 'N', 'T', '#0', 1)")
            execSQL("INSERT INTO usuarios (id, nombre, usuario, password, rol, activo) VALUES (1, 'U', 'u', 'p', 'ADMIN', 1)")
            execSQL("INSERT INTO productos (id, nombre, codigo, precioVenta, costoUnitario, stock, stockMinimo, categoria, fotoUrl, empresaId) VALUES (1, 'P', 'C', 10.50, 5.0, 10, 1, 'C', '', 'c1')")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            8,
            true,
            DatabaseProvider.MIGRATION_4_5,
            DatabaseProvider.MIGRATION_5_6,
            DatabaseProvider.MIGRATION_6_7,
            DatabaseProvider.MIGRATION_7_8
        )

        // Validar transformaciones finales
        db.query("SELECT rol FROM usuarios WHERE id = 1").use {
            it.moveToFirst()
            assertEquals("PROPIETARIO", it.getString(0))
        }
        db.query("SELECT precioVenta FROM productos WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(1050L, it.getLong(0))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrate15To16() {
        helper.createDatabase(TEST_DB, 15).apply {
            execSQL("INSERT INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, activa, nit, direccion, telefono, fechaCreacion) VALUES ('c1', 'Empresa', 'TIENDA', '#2563EB', 1, '', '', '', 0)")
            execSQL("INSERT INTO usuarios (id, nombre, usuario, password, passwordHash, passwordSalt, rol, empresaId, activo, requiereCambioClave, fechaCreacion) VALUES (1, 'Admin', 'admin', '', 'hash', 'salt', 'PROPIETARIO', 'c1', 1, 0, 1000)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            DatabaseProvider.MIGRATION_15_16
        )

        db.query("SELECT intentosFallidos, bloqueadoHasta, ultimoLogin FROM usuarios WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertTrue(it.isNull(1))
            assertTrue(it.isNull(2))
        }
    }

    @Test
    @Throws(IOException::class)
    fun migrateChain14To16() {
        helper.createDatabase(TEST_DB, 14).apply {
            execSQL("INSERT INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, activa, nit, direccion, telefono, fechaCreacion) VALUES ('c1', 'Empresa', 'TIENDA', '#2563EB', 1, '', '', '', 0)")
            execSQL("INSERT INTO usuarios (id, nombre, usuario, password, passwordHash, passwordSalt, rol, empresaId, activo, requiereCambioClave, fechaCreacion) VALUES (1, 'Admin', 'admin', '', 'hash', 'salt', 'PROPIETARIO', 'c1', 1, 0, 1000)")
            execSQL("INSERT INTO usuario_empresas (usuarioId, empresaId, rol, activo) VALUES (1, 'c1', 'PROPIETARIO', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            16,
            true,
            DatabaseProvider.MIGRATION_14_15,
            DatabaseProvider.MIGRATION_15_16
        )

        db.query("SELECT intentosFallidos, bloqueadoHasta, ultimoLogin FROM usuarios WHERE id = 1").use {
            it.moveToFirst()
            assertEquals(0, it.getInt(0))
            assertTrue(it.isNull(1))
            assertTrue(it.isNull(2))
        }
    }
}
