package com.multipos.app.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseProvider {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "multipos.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16)
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    createSingleOpenCashSessionIndex(db)
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    createSingleOpenCashSessionIndex(db)
                }
            })
            .build()
            .also { instance = it }
    }

    val MIGRATION_8_9: Migration = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // 1. Comprobar duplicados antes de modificar
            database.query("SELECT empresaId, TRIM(documento) as trimmed_doc, COUNT(*) as count FROM clientes GROUP BY empresaId, trimmed_doc HAVING count > 1").use { cursor ->
                if (cursor.count > 0) {
                    // No incluir datos sensibles en el mensaje
                    throw IllegalStateException("No se puede migrar: existen ${cursor.count} grupos de clientes con documentos duplicados en la misma empresa.")
                }
            }

            // 2. Normalizar documentos existentes
            database.execSQL("UPDATE clientes SET documento = TRIM(documento)")

            // 3. Crear el índice único
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_clientes_empresaId_documento ON clientes (empresaId, documento)")
        }
    }

    val MIGRATION_9_10: Migration = object : Migration(9, 10) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE productos ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
        }
    }

    val MIGRATION_10_11: Migration = object : Migration(10, 11) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN pinHash TEXT")
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN pinSalt TEXT")
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN fechaVencimiento INTEGER")
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN intentosFallidos INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN bloqueadaHasta INTEGER")
            database.execSQL("ALTER TABLE credenciales_clientes ADD COLUMN ultimoUso INTEGER")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credenciales_clientes_empresaId_estado ON credenciales_clientes(empresaId, estado)")
            database.execSQL("CREATE TABLE IF NOT EXISTS auditoria (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, empresaId TEXT NOT NULL, usuarioId INTEGER, accion TEXT NOT NULL, entidad TEXT NOT NULL, entidadId TEXT, detalle TEXT NOT NULL, fecha INTEGER NOT NULL)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_auditoria_empresaId_fecha ON auditoria(empresaId, fecha)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_auditoria_empresaId_accion ON auditoria(empresaId, accion)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_auditoria_usuarioId ON auditoria(usuarioId)")
        }
    }

    val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS empresas (id TEXT NOT NULL PRIMARY KEY, nombre TEXT NOT NULL, tipoNegocio TEXT NOT NULL, colorPrimarioHex TEXT NOT NULL, logoUri TEXT, activa INTEGER NOT NULL)")
            database.execSQL("INSERT OR IGNORE INTO empresas (id, nombre, tipoNegocio, colorPrimarioHex, logoUri, activa) VALUES ('default-company', 'Mi empresa', 'TIENDA', '#2563EB', NULL, 1)")
            database.execSQL("ALTER TABLE productos ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("ALTER TABLE clientes ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("ALTER TABLE ventas ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("ALTER TABLE detalle_ventas ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("ALTER TABLE abonos ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_productos_empresaId ON productos(empresaId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_clientes_empresaId ON clientes(empresaId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_empresaId ON ventas(empresaId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_detalle_ventas_empresaId ON detalle_ventas(empresaId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_abonos_empresaId ON abonos(empresaId)")
        }
    }

    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE ventas ADD COLUMN subtotal REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE ventas ADD COLUMN descuento REAL NOT NULL DEFAULT 0.0")
            database.execSQL("ALTER TABLE ventas ADD COLUMN impuesto REAL NOT NULL DEFAULT 0.0")
            database.execSQL("UPDATE ventas SET subtotal = total WHERE subtotal = 0.0 AND descuento = 0.0 AND impuesto = 0.0")
        }
    }

    val MIGRATION_3_4: Migration = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE abonos ADD COLUMN idCliente INTEGER NOT NULL DEFAULT 0")
        }
    }

    val MIGRATION_4_5: Migration = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE empresas ADD COLUMN nit TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE empresas ADD COLUMN direccion TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE empresas ADD COLUMN telefono TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE empresas ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE usuarios ADD COLUMN passwordHash TEXT")
            database.execSQL("ALTER TABLE usuarios ADD COLUMN passwordSalt TEXT")
            database.execSQL("ALTER TABLE usuarios ADD COLUMN empresaId TEXT NOT NULL DEFAULT 'default-company'")
            database.execSQL("ALTER TABLE usuarios ADD COLUMN requiereCambioClave INTEGER NOT NULL DEFAULT 1")
            database.execSQL("ALTER TABLE usuarios ADD COLUMN fechaCreacion INTEGER NOT NULL DEFAULT 0")
            database.execSQL("UPDATE usuarios SET rol = 'PROPIETARIO' WHERE rol = 'ADMIN'")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_usuarios_usuario ON usuarios(usuario)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_usuarios_empresaId ON usuarios(empresaId)")
            database.execSQL("CREATE TABLE IF NOT EXISTS usuario_empresas (usuarioId INTEGER NOT NULL, empresaId TEXT NOT NULL, rol TEXT NOT NULL, activo INTEGER NOT NULL, fechaCreacion INTEGER NOT NULL, PRIMARY KEY(usuarioId, empresaId), FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_usuario_empresas_usuarioId ON usuario_empresas(usuarioId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_usuario_empresas_empresaId ON usuario_empresas(empresaId)")
            database.execSQL("INSERT OR IGNORE INTO usuario_empresas (usuarioId, empresaId, rol, activo, fechaCreacion) SELECT id, empresaId, rol, activo, fechaCreacion FROM usuarios")
        }
    }

    val MIGRATION_5_6: Migration = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE productos ADD COLUMN codigoBarras TEXT")
            database.execSQL("ALTER TABLE productos ADD COLUMN tipoCodigo TEXT")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_empresaId_codigo ON productos(empresaId, codigo)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_productos_empresaId_codigoBarras ON productos(empresaId, codigoBarras)")
        }
    }

    val MIGRATION_6_7: Migration = object : Migration(6, 7) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE clientes ADD COLUMN creditoHabilitado INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE clientes ADD COLUMN estadoCredito TEXT NOT NULL DEFAULT 'NO_SOLICITADO'")
            database.execSQL("ALTER TABLE clientes ADD COLUMN fechaInscripcion INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE clientes ADD COLUMN fechaAprobacion INTEGER")
            database.execSQL("ALTER TABLE clientes ADD COLUMN usuarioAproboId INTEGER")
            database.execSQL("ALTER TABLE clientes ADD COLUMN activo INTEGER NOT NULL DEFAULT 1")
            database.execSQL("UPDATE clientes SET estadoCredito = 'PENDIENTE' WHERE limiteCredito > 0")
            database.execSQL("CREATE TABLE IF NOT EXISTS credenciales_clientes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, clienteId INTEGER NOT NULL, empresaId TEXT NOT NULL, credentialId TEXT NOT NULL, version INTEGER NOT NULL, estado TEXT NOT NULL, fechaEmision INTEGER NOT NULL, fechaRevocacion INTEGER, emitidaPorUsuarioId INTEGER NOT NULL, FOREIGN KEY(clienteId) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE CASCADE, FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credenciales_clientes_clienteId ON credenciales_clientes(clienteId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_credenciales_clientes_empresaId ON credenciales_clientes(empresaId)")
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_credenciales_clientes_credentialId ON credenciales_clientes(credentialId)")
        }
    }

    val MIGRATION_7_8: Migration = object : Migration(7, 8) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE credenciales_clientes_backup (
                    id INTEGER PRIMARY KEY NOT NULL,
                    clienteId INTEGER NOT NULL,
                    empresaId TEXT NOT NULL,
                    credentialId TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    fechaEmision INTEGER NOT NULL,
                    fechaRevocacion INTEGER,
                    emitidaPorUsuarioId INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("INSERT INTO credenciales_clientes_backup SELECT id, clienteId, empresaId, credentialId, version, estado, fechaEmision, fechaRevocacion, emitidaPorUsuarioId FROM credenciales_clientes")
            database.execSQL("DROP TABLE credenciales_clientes")

            database.execSQL("""
                CREATE TABLE clientes_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nombre TEXT NOT NULL,
                    documento TEXT NOT NULL,
                    telefono TEXT NOT NULL,
                    direccion TEXT NOT NULL,
                    limiteCredito INTEGER NOT NULL,
                    creditoActual INTEGER NOT NULL,
                    creditoHabilitado INTEGER NOT NULL,
                    estadoCredito TEXT NOT NULL,
                    fechaInscripcion INTEGER NOT NULL,
                    fechaAprobacion INTEGER,
                    usuarioAproboId INTEGER,
                    activo INTEGER NOT NULL,
                    empresaId TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO clientes_new
                SELECT id, nombre, documento, telefono, direccion,
                       CAST(ROUND(limiteCredito * 100.0) AS INTEGER),
                       CAST(ROUND(creditoActual * 100.0) AS INTEGER),
                       creditoHabilitado, estadoCredito, fechaInscripcion,
                       fechaAprobacion, usuarioAproboId, activo, empresaId
                FROM clientes
            """.trimIndent())
            database.execSQL("DROP TABLE clientes")
            database.execSQL("ALTER TABLE clientes_new RENAME TO clientes")
            database.execSQL("CREATE INDEX index_clientes_empresaId ON clientes(empresaId)")

            database.execSQL("""
                CREATE TABLE credenciales_clientes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    clienteId INTEGER NOT NULL,
                    empresaId TEXT NOT NULL,
                    credentialId TEXT NOT NULL,
                    version INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    fechaEmision INTEGER NOT NULL,
                    fechaRevocacion INTEGER,
                    emitidaPorUsuarioId INTEGER NOT NULL,
                    FOREIGN KEY(clienteId) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            database.execSQL("INSERT INTO credenciales_clientes SELECT id, clienteId, empresaId, credentialId, version, estado, fechaEmision, fechaRevocacion, emitidaPorUsuarioId FROM credenciales_clientes_backup")
            database.execSQL("DROP TABLE credenciales_clientes_backup")
            database.execSQL("CREATE INDEX index_credenciales_clientes_clienteId ON credenciales_clientes(clienteId)")
            database.execSQL("CREATE INDEX index_credenciales_clientes_empresaId ON credenciales_clientes(empresaId)")
            database.execSQL("CREATE UNIQUE INDEX index_credenciales_clientes_credentialId ON credenciales_clientes(credentialId)")

            database.execSQL("""
                CREATE TABLE productos_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nombre TEXT NOT NULL,
                    codigo TEXT NOT NULL,
                    precioVenta INTEGER NOT NULL,
                    costoUnitario INTEGER NOT NULL,
                    stock INTEGER NOT NULL,
                    stockMinimo INTEGER NOT NULL,
                    categoria TEXT NOT NULL,
                    fotoUrl TEXT NOT NULL,
                    codigoBarras TEXT,
                    tipoCodigo TEXT,
                    empresaId TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO productos_new
                SELECT id, nombre, codigo,
                       CAST(ROUND(precioVenta * 100.0) AS INTEGER),
                       CAST(ROUND(costoUnitario * 100.0) AS INTEGER),
                       stock, stockMinimo, categoria, fotoUrl, codigoBarras, tipoCodigo, empresaId
                FROM productos
            """.trimIndent())
            database.execSQL("DROP TABLE productos")
            database.execSQL("ALTER TABLE productos_new RENAME TO productos")
            database.execSQL("CREATE INDEX index_productos_empresaId ON productos(empresaId)")
            database.execSQL("CREATE UNIQUE INDEX index_productos_empresaId_codigo ON productos(empresaId, codigo)")
            database.execSQL("CREATE UNIQUE INDEX index_productos_empresaId_codigoBarras ON productos(empresaId, codigoBarras)")

            database.execSQL("""
                CREATE TABLE ventas_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    fecha INTEGER NOT NULL,
                    tipoPago TEXT NOT NULL,
                    total INTEGER NOT NULL,
                    subtotal INTEGER NOT NULL,
                    descuento INTEGER NOT NULL,
                    impuesto INTEGER NOT NULL,
                    idCliente INTEGER,
                    idUsuario INTEGER NOT NULL,
                    estado TEXT NOT NULL,
                    empresaId TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO ventas_new
                SELECT id, fecha, tipoPago,
                       CAST(ROUND(total * 100.0) AS INTEGER),
                       CAST(ROUND(subtotal * 100.0) AS INTEGER),
                       CAST(ROUND(descuento * 100.0) AS INTEGER),
                       CAST(ROUND(impuesto * 100.0) AS INTEGER),
                       idCliente, idUsuario, estado, empresaId
                FROM ventas
            """.trimIndent())
            database.execSQL("DROP TABLE ventas")
            database.execSQL("ALTER TABLE ventas_new RENAME TO ventas")
            database.execSQL("CREATE INDEX index_ventas_empresaId ON ventas(empresaId)")

            database.execSQL("""
                CREATE TABLE detalle_ventas_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    idVenta INTEGER NOT NULL,
                    idProducto INTEGER NOT NULL,
                    cantidad INTEGER NOT NULL,
                    precioUnitario INTEGER NOT NULL,
                    subtotal INTEGER NOT NULL,
                    empresaId TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO detalle_ventas_new
                SELECT id, idVenta, idProducto, cantidad,
                       CAST(ROUND(precioUnitario * 100.0) AS INTEGER),
                       CAST(ROUND(subtotal * 100.0) AS INTEGER), empresaId
                FROM detalle_ventas
            """.trimIndent())
            database.execSQL("DROP TABLE detalle_ventas")
            database.execSQL("ALTER TABLE detalle_ventas_new RENAME TO detalle_ventas")
            database.execSQL("CREATE INDEX index_detalle_ventas_empresaId ON detalle_ventas(empresaId)")

            database.execSQL("""
                CREATE TABLE abonos_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    idVenta INTEGER NOT NULL,
                    monto INTEGER NOT NULL,
                    fecha INTEGER NOT NULL,
                    nota TEXT NOT NULL,
                    empresaId TEXT NOT NULL,
                    idCliente INTEGER NOT NULL
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO abonos_new
                SELECT id, idVenta, CAST(ROUND(monto * 100.0) AS INTEGER),
                       fecha, nota, empresaId, idCliente
                FROM abonos
            """.trimIndent())
            database.execSQL("DROP TABLE abonos")
            database.execSQL("ALTER TABLE abonos_new RENAME TO abonos")
            database.execSQL("CREATE INDEX index_abonos_empresaId ON abonos(empresaId)")
        }
    }

    val MIGRATION_11_12: Migration = object : Migration(11, 12) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE caja_sesiones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    abiertaPorUsuarioId INTEGER NOT NULL,
                    cerradaPorUsuarioId INTEGER,
                    fechaApertura INTEGER NOT NULL,
                    fechaCierre INTEGER,
                    montoApertura INTEGER NOT NULL,
                    montoEsperadoCierre INTEGER,
                    montoContadoCierre INTEGER,
                    diferenciaCierre INTEGER,
                    estado TEXT NOT NULL,
                    notaCierre TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_caja_sesiones_empresaId_estado ON caja_sesiones(empresaId, estado)")
            createSingleOpenCashSessionIndex(database)

            database.execSQL("""
                CREATE TABLE movimientos_caja (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    cajaSesionId INTEGER NOT NULL,
                    empresaId TEXT NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    monto INTEGER NOT NULL,
                    ventaId INTEGER,
                    abonoId INTEGER,
                    devolucionId INTEGER,
                    concepto TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    detalle TEXT NOT NULL
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_caja_cajaSesionId_fecha ON movimientos_caja(cajaSesionId, fecha)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_movimientos_caja_empresaId_fecha ON movimientos_caja(empresaId, fecha)")

            database.execSQL("ALTER TABLE ventas ADD COLUMN cajaSesionId INTEGER")
            database.execSQL("ALTER TABLE ventas ADD COLUMN anuladaPorUsuarioId INTEGER")
            database.execSQL("ALTER TABLE ventas ADD COLUMN fechaAnulacion INTEGER")
            database.execSQL("ALTER TABLE ventas ADD COLUMN motivoAnulacion TEXT")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_ventas_cajaSesionId ON ventas(cajaSesionId)")
        }
    }

    val MIGRATION_12_13: Migration = object : Migration(12, 13) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE detalle_ventas ADD COLUMN costoUnitario INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE detalle_ventas ADD COLUMN nombreProductoSnapshot TEXT NOT NULL DEFAULT ''")
            database.execSQL("UPDATE detalle_ventas SET costoUnitario = COALESCE((SELECT costoUnitario FROM productos WHERE productos.id = detalle_ventas.idProducto), 0), nombreProductoSnapshot = COALESCE((SELECT nombre FROM productos WHERE productos.id = detalle_ventas.idProducto), 'Producto #' || idProducto)")

            database.execSQL("""
                CREATE TABLE movimientos_inventario (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    productoId INTEGER NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    cantidadFirmada INTEGER NOT NULL,
                    stockAnterior INTEGER NOT NULL,
                    stockPosterior INTEGER NOT NULL,
                    ventaId INTEGER,
                    devolucionId INTEGER,
                    motivo TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX index_movimientos_inventario_empresaId_fecha ON movimientos_inventario(empresaId, fecha)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_productoId ON movimientos_inventario(productoId)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_ventaId ON movimientos_inventario(ventaId)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_devolucionId ON movimientos_inventario(devolucionId)")

            database.execSQL("""
                CREATE TABLE devoluciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    ventaId INTEGER NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    cajaSesionId INTEGER,
                    monto INTEGER NOT NULL,
                    medioReembolso TEXT NOT NULL,
                    estadoReembolso TEXT NOT NULL,
                    motivo TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(ventaId) REFERENCES ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(cajaSesionId) REFERENCES caja_sesiones(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX index_devoluciones_empresaId_fecha ON devoluciones(empresaId, fecha)")
            database.execSQL("CREATE INDEX index_devoluciones_ventaId ON devoluciones(ventaId)")
            database.execSQL("CREATE INDEX index_devoluciones_cajaSesionId ON devoluciones(cajaSesionId)")

            database.execSQL("""
                CREATE TABLE detalle_devoluciones (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    devolucionId INTEGER NOT NULL,
                    detalleVentaId INTEGER NOT NULL,
                    productoId INTEGER NOT NULL,
                    cantidad INTEGER NOT NULL,
                    precioUnitario INTEGER NOT NULL,
                    subtotal INTEGER NOT NULL,
                    FOREIGN KEY(devolucionId) REFERENCES devoluciones(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(detalleVentaId) REFERENCES detalle_ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX index_detalle_devoluciones_devolucionId ON detalle_devoluciones(devolucionId)")
            database.execSQL("CREATE INDEX index_detalle_devoluciones_detalleVentaId ON detalle_devoluciones(detalleVentaId)")
            database.execSQL("CREATE INDEX index_detalle_devoluciones_productoId ON detalle_devoluciones(productoId)")
        }
    }

    val MIGRATION_13_14: Migration = object : Migration(13, 14) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("""
                CREATE TABLE devoluciones_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    ventaId INTEGER NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    cajaSesionId INTEGER,
                    monto INTEGER NOT NULL,
                    medioReembolso TEXT NOT NULL,
                    estadoReembolso TEXT NOT NULL,
                    motivo TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(ventaId) REFERENCES ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(cajaSesionId) REFERENCES caja_sesiones(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("INSERT INTO devoluciones_new SELECT id, empresaId, ventaId, usuarioId, cajaSesionId, monto, medioReembolso, estadoReembolso, motivo, fecha FROM devoluciones")
            database.execSQL("DROP TABLE devoluciones")
            database.execSQL("ALTER TABLE devoluciones_new RENAME TO devoluciones")
            database.execSQL("CREATE INDEX index_devoluciones_empresaId_fecha ON devoluciones(empresaId, fecha)")
            database.execSQL("CREATE INDEX index_devoluciones_ventaId ON devoluciones(ventaId)")
            database.execSQL("CREATE INDEX index_devoluciones_cajaSesionId ON devoluciones(cajaSesionId)")

            database.execSQL("""
                CREATE TABLE movimientos_inventario_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    productoId INTEGER NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    cantidadFirmada INTEGER NOT NULL,
                    stockAnterior INTEGER NOT NULL,
                    stockPosterior INTEGER NOT NULL,
                    ventaId INTEGER,
                    devolucionId INTEGER,
                    motivo TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(productoId) REFERENCES productos(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(ventaId) REFERENCES ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(devolucionId) REFERENCES devoluciones(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("INSERT INTO movimientos_inventario_new SELECT id, empresaId, productoId, usuarioId, tipo, cantidadFirmada, stockAnterior, stockPosterior, ventaId, devolucionId, motivo, fecha FROM movimientos_inventario")
            database.execSQL("DROP TABLE movimientos_inventario")
            database.execSQL("ALTER TABLE movimientos_inventario_new RENAME TO movimientos_inventario")
            database.execSQL("CREATE INDEX index_movimientos_inventario_empresaId_fecha ON movimientos_inventario(empresaId, fecha)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_productoId ON movimientos_inventario(productoId)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_ventaId ON movimientos_inventario(ventaId)")
            database.execSQL("CREATE INDEX index_movimientos_inventario_devolucionId ON movimientos_inventario(devolucionId)")
        }
    }

    val MIGRATION_14_15: Migration = object : Migration(14, 15) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val now = System.currentTimeMillis()

            database.query(
                "SELECT COUNT(*) FROM abonos a WHERE COALESCE(NULLIF(a.idCliente, 0), (SELECT v.idCliente FROM ventas v WHERE v.id = a.idVenta), 0) = 0"
            ).use { cursor ->
                cursor.moveToFirst()
                val unresolved = cursor.getInt(0)
                if (unresolved > 0) {
                    throw IllegalStateException("No se puede migrar: existen $unresolved abonos sin cliente resuelto.")
                }
            }
            database.query(
                "SELECT COUNT(DISTINCT a.empresaId) FROM abonos a WHERE NOT EXISTS (" +
                    "SELECT 1 FROM usuario_empresas ue JOIN usuarios u ON u.id = ue.usuarioId " +
                    "WHERE ue.empresaId = a.empresaId AND ue.rol = 'PROPIETARIO' AND ue.activo = 1)"
            ).use { cursor ->
                cursor.moveToFirst()
                val companies = cursor.getInt(0)
                if (companies > 0) {
                    throw IllegalStateException("No se puede migrar: $companies empresas con abonos no tienen propietario activo.")
                }
            }

            database.execSQL("""
                CREATE TABLE abonos_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    idCliente INTEGER NOT NULL,
                    idVenta INTEGER,
                    cajaSesionId INTEGER,
                    usuarioId INTEGER NOT NULL,
                    monto INTEGER NOT NULL,
                    medioPago TEXT NOT NULL,
                    fecha INTEGER NOT NULL,
                    nota TEXT NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(idCliente) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(idVenta) REFERENCES ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(cajaSesionId) REFERENCES caja_sesiones(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("""
                INSERT INTO abonos_new (id, empresaId, idCliente, idVenta, cajaSesionId, usuarioId, monto, medioPago, fecha, nota)
                SELECT a.id, a.empresaId,
                       COALESCE(NULLIF(a.idCliente, 0), (SELECT v.idCliente FROM ventas v WHERE v.id = a.idVenta)),
                       CASE WHEN a.idVenta = 0 THEN NULL ELSE a.idVenta END,
                       NULL,
                       (SELECT u.id FROM usuarios u JOIN usuario_empresas ue ON ue.usuarioId = u.id
                        WHERE ue.empresaId = a.empresaId AND ue.rol = 'PROPIETARIO' AND ue.activo = 1
                        ORDER BY u.fechaCreacion ASC, u.id ASC LIMIT 1),
                       a.monto, 'EFECTIVO', a.fecha, a.nota
                FROM abonos a
            """.trimIndent())
            database.execSQL("DROP TABLE abonos")
            database.execSQL("ALTER TABLE abonos_new RENAME TO abonos")
            database.execSQL("CREATE INDEX index_abonos_empresaId ON abonos(empresaId)")
            database.execSQL("CREATE INDEX index_abonos_empresaId_idCliente ON abonos(empresaId, idCliente)")
            database.execSQL("CREATE INDEX index_abonos_idVenta ON abonos(idVenta)")
            database.execSQL("CREATE INDEX index_abonos_cajaSesionId ON abonos(cajaSesionId)")
            database.execSQL("CREATE INDEX index_abonos_usuarioId ON abonos(usuarioId)")

            database.execSQL("""
                CREATE TABLE movimientos_credito (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    empresaId TEXT NOT NULL,
                    clienteId INTEGER NOT NULL,
                    usuarioId INTEGER NOT NULL,
                    tipo TEXT NOT NULL,
                    importeFirmado INTEGER NOT NULL,
                    saldoPosterior INTEGER NOT NULL,
                    ventaId INTEGER,
                    abonoId INTEGER,
                    devolucionId INTEGER,
                    fecha INTEGER NOT NULL,
                    nota TEXT NOT NULL,
                    FOREIGN KEY(empresaId) REFERENCES empresas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(clienteId) REFERENCES clientes(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(usuarioId) REFERENCES usuarios(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(ventaId) REFERENCES ventas(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(abonoId) REFERENCES abonos(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                    FOREIGN KEY(devolucionId) REFERENCES devoluciones(id) ON UPDATE NO ACTION ON DELETE RESTRICT
                )
            """.trimIndent())
            database.execSQL("CREATE INDEX index_movimientos_credito_empresaId_clienteId_fecha ON movimientos_credito(empresaId, clienteId, fecha)")
            database.execSQL("CREATE INDEX index_movimientos_credito_ventaId ON movimientos_credito(ventaId)")
            database.execSQL("CREATE INDEX index_movimientos_credito_abonoId ON movimientos_credito(abonoId)")
            database.execSQL("CREATE INDEX index_movimientos_credito_devolucionId ON movimientos_credito(devolucionId)")

            data class LedgerEvent(
                val fecha: Long,
                val seq: Int,
                val sortId: Long,
                val empresaId: String,
                val clienteId: Int,
                val usuarioId: Int,
                val tipo: String,
                val importe: Long,
                val ventaId: Int?,
                val abonoId: Long?,
                val devolucionId: Long?
            )

            val events = mutableListOf<LedgerEvent>()
            database.query("SELECT id, empresaId, idCliente, idUsuario, total, fecha, estado FROM ventas WHERE tipoPago = 'CREDITO' AND idCliente IS NOT NULL").use { c ->
                val idI = c.getColumnIndexOrThrow("id")
                val empI = c.getColumnIndexOrThrow("empresaId")
                val cliI = c.getColumnIndexOrThrow("idCliente")
                val usrI = c.getColumnIndexOrThrow("idUsuario")
                val totalI = c.getColumnIndexOrThrow("total")
                val fecI = c.getColumnIndexOrThrow("fecha")
                val estI = c.getColumnIndexOrThrow("estado")
                while (c.moveToNext()) {
                    val saleId = c.getInt(idI)
                    events.add(
                        LedgerEvent(
                            fecha = c.getLong(fecI), seq = 0, sortId = saleId.toLong(),
                            empresaId = c.getString(empI), clienteId = c.getInt(cliI), usuarioId = c.getInt(usrI),
                            tipo = com.multipos.app.data.entities.MovimientoCredito.TIPO_VENTA_CREDITO,
                            importe = c.getLong(totalI), ventaId = saleId, abonoId = null, devolucionId = null
                        )
                    )
                    if (c.getString(estI) == com.multipos.app.data.entities.Venta.ESTADO_ANULADA) {
                        events.add(
                            LedgerEvent(
                                fecha = c.getLong(fecI), seq = 3, sortId = saleId.toLong(),
                                empresaId = c.getString(empI), clienteId = c.getInt(cliI), usuarioId = c.getInt(usrI),
                                tipo = com.multipos.app.data.entities.MovimientoCredito.TIPO_ANULACION,
                                importe = -c.getLong(totalI), ventaId = saleId, abonoId = null, devolucionId = null
                            )
                        )
                    }
                }
            }
            database.query(
                "SELECT d.id, d.ventaId, d.monto, d.fecha, v.idCliente, v.idUsuario FROM devoluciones d " +
                    "JOIN ventas v ON v.id = d.ventaId WHERE d.medioReembolso = 'CREDITO'"
            ).use { c ->
                val idI = c.getColumnIndexOrThrow("id")
                val venI = c.getColumnIndexOrThrow("ventaId")
                val monI = c.getColumnIndexOrThrow("monto")
                val fecI = c.getColumnIndexOrThrow("fecha")
                val cliI = c.getColumnIndexOrThrow("idCliente")
                val usrI = c.getColumnIndexOrThrow("idUsuario")
                while (c.moveToNext()) {
                    events.add(
                        LedgerEvent(
                            fecha = c.getLong(fecI), seq = 2, sortId = c.getLong(idI),
                            empresaId = "", clienteId = c.getInt(cliI), usuarioId = c.getInt(usrI),
                            tipo = com.multipos.app.data.entities.MovimientoCredito.TIPO_DEVOLUCION,
                            importe = -c.getLong(monI), ventaId = c.getInt(venI), abonoId = null, devolucionId = c.getLong(idI)
                        )
                    )
                }
            }
            database.query("SELECT id, empresaId, idCliente, usuarioId, monto, fecha FROM abonos").use { c ->
                val idI = c.getColumnIndexOrThrow("id")
                val empI = c.getColumnIndexOrThrow("empresaId")
                val cliI = c.getColumnIndexOrThrow("idCliente")
                val usrI = c.getColumnIndexOrThrow("usuarioId")
                val monI = c.getColumnIndexOrThrow("monto")
                val fecI = c.getColumnIndexOrThrow("fecha")
                while (c.moveToNext()) {
                    events.add(
                        LedgerEvent(
                            fecha = c.getLong(fecI), seq = 1, sortId = c.getLong(idI),
                            empresaId = c.getString(empI), clienteId = c.getInt(cliI), usuarioId = c.getInt(usrI),
                            tipo = com.multipos.app.data.entities.MovimientoCredito.TIPO_ABONO,
                            importe = -c.getLong(monI), ventaId = null, abonoId = c.getLong(idI), devolucionId = null
                        )
                    )
                }
            }

            val saldos = mutableMapOf<Pair<String, Int>, Long>()
            for (event in events.sortedWith(compareBy({ it.fecha }, { it.seq }, { it.sortId }))) {
                val key = event.empresaId to event.clienteId
                val anterior = saldos.getOrDefault(key, 0L)
                val nuevo = anterior + event.importe
                if (nuevo < 0) {
                    throw IllegalStateException("No se puede migrar: saldo de crédito negativo para cliente ${event.clienteId}.")
                }
                saldos[key] = nuevo
                database.execSQL(
                    "INSERT INTO movimientos_credito (empresaId, clienteId, usuarioId, tipo, importeFirmado, saldoPosterior, ventaId, abonoId, devolucionId, fecha, nota) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(
                        event.empresaId, event.clienteId, event.usuarioId, event.tipo,
                        event.importe, nuevo, event.ventaId, event.abonoId, event.devolucionId, event.fecha, ""
                    )
                )
            }

            var mismatched = 0
            var checked = 0
            for ((key, saldo) in saldos) {
                checked++
                database.query("SELECT creditoActual FROM clientes WHERE id = ? AND empresaId = ?", arrayOf(key.second, key.first)).use { c ->
                    if (c.moveToFirst() && c.getLong(0) != saldo) mismatched++
                }
            }
            if (mismatched > 0) {
                throw IllegalStateException("No se puede migrar: $mismatched de $checked clientes con saldo de crédito inconsistente.")
            }

            database.execSQL(
                "INSERT INTO auditoria (empresaId, usuarioId, accion, entidad, entidadId, detalle, fecha) " +
                    "SELECT DISTINCT a.empresaId, " +
                    "(SELECT u.id FROM usuarios u JOIN usuario_empresas ue ON ue.usuarioId = u.id " +
                    "WHERE ue.empresaId = a.empresaId AND ue.rol = 'PROPIETARIO' AND ue.activo = 1 " +
                    "ORDER BY u.fechaCreacion ASC, u.id ASC LIMIT 1), " +
                    "'CONFIGURACION', 'abono', 'migracion', 'abonos legados asignados al propietario mas antiguo', $now " +
                    "FROM abonos a"
            )
        }
    }

val MIGRATION_15_16: Migration = object : Migration(15, 16) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Seguridad de login: intentos fallidos, bloqueo persistente y último login.
            // No destructiva: solo agrega columnas con valores por defecto compatibles.
            database.execSQL(
                "ALTER TABLE usuarios ADD COLUMN intentosFallidos INTEGER NOT NULL DEFAULT 0"
            )
            database.execSQL(
                "ALTER TABLE usuarios ADD COLUMN bloqueadoHasta INTEGER"
            )
            database.execSQL(
                "ALTER TABLE usuarios ADD COLUMN ultimoLogin INTEGER"
            )
        }
    }
    private fun createSingleOpenCashSessionIndex(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS index_caja_sesiones_una_abierta_por_empresa " +
                "ON caja_sesiones(empresaId) WHERE estado = 'ABIERTA'"
        )
    }
}
