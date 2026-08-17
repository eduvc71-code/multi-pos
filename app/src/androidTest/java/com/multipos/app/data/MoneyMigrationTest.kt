package com.multipos.app.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MoneyMigrationTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersion7Database() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(TEST_DATABASE)
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onCreate(db: SupportSQLiteDatabase) = createVersion7Schema(db)
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        seedVersion7Data(helper.writableDatabase)
    }

    @After
    fun closeDatabase() {
        helper.close()
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationConvertsEveryMoneyColumnAndPreservesCreditCredential() {
        val db = helper.writableDatabase

        DatabaseProvider.MIGRATION_7_8.migrate(db)

        assertEquals(1_234L, queryLong(db, "SELECT precioVenta FROM productos WHERE id = 1"))
        assertEquals(567L, queryLong(db, "SELECT costoUnitario FROM productos WHERE id = 1"))
        assertEquals(2_345L, queryLong(db, "SELECT limiteCredito FROM clientes WHERE id = 1"))
        assertEquals(456L, queryLong(db, "SELECT creditoActual FROM clientes WHERE id = 1"))
        assertEquals(1_345L, queryLong(db, "SELECT total FROM ventas WHERE id = 1"))
        assertEquals(1_234L, queryLong(db, "SELECT subtotal FROM ventas WHERE id = 1"))
        assertEquals(100L, queryLong(db, "SELECT descuento FROM ventas WHERE id = 1"))
        assertEquals(211L, queryLong(db, "SELECT impuesto FROM ventas WHERE id = 1"))
        assertEquals(1_234L, queryLong(db, "SELECT precioUnitario FROM detalle_ventas WHERE id = 1"))
        assertEquals(1_234L, queryLong(db, "SELECT subtotal FROM detalle_ventas WHERE id = 1"))
        assertEquals(300L, queryLong(db, "SELECT monto FROM abonos WHERE id = 1"))
        assertEquals(1L, queryLong(db, "SELECT COUNT(*) FROM credenciales_clientes WHERE credentialId = 'credential-1'"))
        assertEquals("INTEGER", columnType(db, "productos", "precioVenta"))
        assertEquals("INTEGER", columnType(db, "clientes", "limiteCredito"))
        assertEquals("INTEGER", columnType(db, "ventas", "total"))
    }

    private fun createVersion7Schema(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE empresas (id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, tipoNegocio TEXT NOT NULL, colorPrimarioHex TEXT NOT NULL, logoUri TEXT, activa INTEGER NOT NULL, nit TEXT NOT NULL, direccion TEXT NOT NULL, telefono TEXT NOT NULL, fechaCreacion INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE clientes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, documento TEXT NOT NULL, telefono TEXT NOT NULL, direccion TEXT NOT NULL, limiteCredito REAL NOT NULL, creditoActual REAL NOT NULL, creditoHabilitado INTEGER NOT NULL, estadoCredito TEXT NOT NULL, fechaInscripcion INTEGER NOT NULL, fechaAprobacion INTEGER, usuarioAproboId INTEGER, activo INTEGER NOT NULL, empresaId TEXT NOT NULL)")
        db.execSQL("CREATE INDEX index_clientes_empresaId ON clientes(empresaId)")
        db.execSQL("CREATE TABLE credenciales_clientes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clienteId INTEGER NOT NULL, empresaId TEXT NOT NULL, credentialId TEXT NOT NULL, version INTEGER NOT NULL, estado TEXT NOT NULL, fechaEmision INTEGER NOT NULL, fechaRevocacion INTEGER, emitidaPorUsuarioId INTEGER NOT NULL, FOREIGN KEY(clienteId) REFERENCES clientes(id) ON DELETE CASCADE, FOREIGN KEY(empresaId) REFERENCES empresas(id) ON DELETE CASCADE)")
        db.execSQL("CREATE INDEX index_credenciales_clientes_clienteId ON credenciales_clientes(clienteId)")
        db.execSQL("CREATE INDEX index_credenciales_clientes_empresaId ON credenciales_clientes(empresaId)")
        db.execSQL("CREATE UNIQUE INDEX index_credenciales_clientes_credentialId ON credenciales_clientes(credentialId)")
        db.execSQL("CREATE TABLE productos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, nombre TEXT NOT NULL, codigo TEXT NOT NULL, precioVenta REAL NOT NULL, costoUnitario REAL NOT NULL, stock INTEGER NOT NULL, stockMinimo INTEGER NOT NULL, categoria TEXT NOT NULL, fotoUrl TEXT NOT NULL, codigoBarras TEXT, tipoCodigo TEXT, empresaId TEXT NOT NULL)")
        db.execSQL("CREATE INDEX index_productos_empresaId ON productos(empresaId)")
        db.execSQL("CREATE UNIQUE INDEX index_productos_empresaId_codigo ON productos(empresaId, codigo)")
        db.execSQL("CREATE UNIQUE INDEX index_productos_empresaId_codigoBarras ON productos(empresaId, codigoBarras)")
        db.execSQL("CREATE TABLE ventas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, fecha INTEGER NOT NULL, tipoPago TEXT NOT NULL, total REAL NOT NULL, subtotal REAL NOT NULL, descuento REAL NOT NULL, impuesto REAL NOT NULL, idCliente INTEGER, idUsuario INTEGER NOT NULL, estado TEXT NOT NULL, empresaId TEXT NOT NULL)")
        db.execSQL("CREATE INDEX index_ventas_empresaId ON ventas(empresaId)")
        db.execSQL("CREATE TABLE detalle_ventas (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idVenta INTEGER NOT NULL, idProducto INTEGER NOT NULL, cantidad INTEGER NOT NULL, precioUnitario REAL NOT NULL, subtotal REAL NOT NULL, empresaId TEXT NOT NULL)")
        db.execSQL("CREATE INDEX index_detalle_ventas_empresaId ON detalle_ventas(empresaId)")
        db.execSQL("CREATE TABLE abonos (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, idVenta INTEGER NOT NULL, monto REAL NOT NULL, fecha INTEGER NOT NULL, nota TEXT NOT NULL, empresaId TEXT NOT NULL, idCliente INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX index_abonos_empresaId ON abonos(empresaId)")
    }

    private fun seedVersion7Data(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO empresas VALUES ('company-a', 'Empresa', 'TIENDA', '#2563EB', NULL, 1, '', '', '', 0)")
        db.execSQL("INSERT INTO clientes VALUES (1, 'Cliente', 'DOC-1', '', '', 23.45, 4.56, 1, 'ACTIVO', 0, 0, 1, 1, 'company-a')")
        db.execSQL("INSERT INTO credenciales_clientes VALUES (1, 1, 'company-a', 'credential-1', 1, 'ACTIVA', 0, NULL, 1)")
        db.execSQL("INSERT INTO productos VALUES (1, 'Producto', 'SKU-1', 12.34, 5.67, 10, 2, 'General', '', 'BAR-1', 'QR', 'company-a')")
        db.execSQL("INSERT INTO ventas VALUES (1, 0, 'EFECTIVO', 13.45, 12.34, 1.00, 2.11, 1, 1, 'COMPLETADA', 'company-a')")
        db.execSQL("INSERT INTO detalle_ventas VALUES (1, 1, 1, 1, 12.34, 12.34, 'company-a')")
        db.execSQL("INSERT INTO abonos VALUES (1, 1, 3.00, 0, 'Abono', 'company-a', 1)")
    }

    private fun queryLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private fun columnType(db: SupportSQLiteDatabase, table: String, column: String): String =
        db.query("PRAGMA table_info($table)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            val typeIndex = cursor.getColumnIndexOrThrow("type")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == column) return@use cursor.getString(typeIndex)
            }
            error("No se encontró $table.$column")
        }

    private companion object {
        const val TEST_DATABASE = "money-migration-test.db"
    }
}
