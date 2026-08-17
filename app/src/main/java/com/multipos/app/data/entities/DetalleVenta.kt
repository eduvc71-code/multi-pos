package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
import androidx.room.Index
 
@Entity(tableName = "detalle_ventas", indices = [Index(value = ["empresaId"])])
data class DetalleVenta( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val idVenta: Int, 
    val idProducto: Int, 
    val cantidad: Int, 
    val precioUnitario: Long,
    val subtotal: Long,
    val costoUnitario: Long = 0,
    val nombreProductoSnapshot: String = "",
    val empresaId: String = Empresa.DEFAULT_ID
) 
