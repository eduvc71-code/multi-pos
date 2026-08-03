package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "ventas") 
data class Venta( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val fecha: Long = System.currentTimeMillis(), 
    val tipoPago: String, 
    val total: Double, 
    val idCliente: Int? = null, 
    val idUsuario: Int, 
    val estado: String = "COMPLETADA" 
) 
