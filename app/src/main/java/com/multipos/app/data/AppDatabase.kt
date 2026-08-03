package com.multipos.app.data  
  
import androidx.room.Database  
import androidx.room.RoomDatabase  
import com.multipos.app.data.dao.ProductoDao  
import com.multipos.app.data.dao.ClienteDao
import com.multipos.app.data.dao.UsuarioDao  
import com.multipos.app.data.dao.VentaDao
import com.multipos.app.data.entities.*  
  
@Database(  
    entities = [  
        Usuario::class,  
        Cliente::class,  
        Producto::class,  
        Venta::class,  
        DetalleVenta::class,  
        Abono::class  
    ],  
    version = 1,
    exportSchema = false
)  
abstract class AppDatabase : RoomDatabase() {  
    abstract fun usuarioDao(): UsuarioDao  
    abstract fun productoDao(): ProductoDao  
    abstract fun clienteDao(): ClienteDao
    abstract fun ventaDao(): VentaDao
}
