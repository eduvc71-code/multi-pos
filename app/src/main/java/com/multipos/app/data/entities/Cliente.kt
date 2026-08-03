package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "clientes") 
data class Cliente( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val nombre: String, 
    val documento: String, 
    val telefono: String = "", 
    val direccion: String = "", 
    val limiteCredito: Double = 0.0, 
    val creditoActual: Double = 0.0 
) 
