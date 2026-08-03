package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "detalle_ventas") 
data class DetalleVenta( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val idVenta: Int, 
    val idProducto: Int, 
    val cantidad: Int, 
    val precioUnitario: Double, 
    val subtotal: Double 
) 
