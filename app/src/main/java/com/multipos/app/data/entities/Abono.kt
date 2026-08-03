package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "abonos") 
data class Abono( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val idVenta: Int, 
    val monto: Double, 
    val fecha: Long = System.currentTimeMillis(), 
    val nota: String = "" 
) 
