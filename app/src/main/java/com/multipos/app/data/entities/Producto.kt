package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "productos") 
data class Producto( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val nombre: String, 
    val codigo: String, 
    val precioVenta: Double, 
    val costoUnitario: Double, 
    val stock: Int, 
    val stockMinimo: Int = 5, 
    val categoria: String = "General", 
    val fotoUrl: String = "" 
) 
