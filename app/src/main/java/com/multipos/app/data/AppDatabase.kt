package com.multipos.app.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.multipos.app.data.dao.ProductoDao
import com.multipos.app.data.dao.ClienteDao
import com.multipos.app.data.dao.UsuarioDao
import com.multipos.app.data.dao.VentaDao
import com.multipos.app.data.dao.EmpresaDao
import com.multipos.app.data.dao.AbonoDao
import com.multipos.app.data.dao.UsuarioEmpresaDao
import com.multipos.app.data.dao.CredencialClienteDao
import com.multipos.app.data.dao.AuditoriaDao
import com.multipos.app.data.dao.CajaSesionDao
import com.multipos.app.data.dao.MovimientoCajaDao
import com.multipos.app.data.dao.MovimientoInventarioDao
import com.multipos.app.data.dao.MovimientoCreditoDao
import com.multipos.app.data.dao.DevolucionDao
import com.multipos.app.data.entities.*

@Database(
    entities = [
        Usuario::class,
        Cliente::class,
        Producto::class,
        Venta::class,
        DetalleVenta::class,
        Abono::class,
        Empresa::class,
        UsuarioEmpresa::class,
        CredencialCliente::class,
        Auditoria::class,
        CajaSesion::class,
        MovimientoCaja::class,
        MovimientoInventario::class,
        Devolucion::class,
        DetalleDevolucion::class,
        MovimientoCredito::class
    ],
    version = 16,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun productoDao(): ProductoDao
    abstract fun clienteDao(): ClienteDao
    abstract fun ventaDao(): VentaDao
    abstract fun empresaDao(): EmpresaDao
    abstract fun abonoDao(): AbonoDao
    abstract fun usuarioEmpresaDao(): UsuarioEmpresaDao
    abstract fun credencialClienteDao(): CredencialClienteDao
    abstract fun auditoriaDao(): AuditoriaDao
    abstract fun cajaSesionDao(): CajaSesionDao
    abstract fun movimientoCajaDao(): MovimientoCajaDao
    abstract fun movimientoInventarioDao(): MovimientoInventarioDao
    abstract fun devolucionDao(): DevolucionDao
    abstract fun movimientoCreditoDao(): MovimientoCreditoDao
}
