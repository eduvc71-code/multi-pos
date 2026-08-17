package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
import androidx.room.Index
 
@Entity(
    tableName = "productos",
    indices = [
        Index(value = ["empresaId"]),
        Index(value = ["empresaId", "codigo"], unique = true),
        Index(value = ["empresaId", "codigoBarras"], unique = true)
    ]
)
data class Producto( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val nombre: String, 
    val codigo: String, 
    val precioVenta: Long,
    val costoUnitario: Long,
    val stock: Int, 
    val stockMinimo: Int = 5, 
    val categoria: String = "General", 
    val fotoUrl: String = "",
    val codigoBarras: String? = null,
    val tipoCodigo: String? = null,
    val empresaId: String = Empresa.DEFAULT_ID,
    val activo: Boolean = true
) 
