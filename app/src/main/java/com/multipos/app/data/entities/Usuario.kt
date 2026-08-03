package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
 
@Entity(tableName = "usuarios") 
data class Usuario( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val nombre: String, 
    val usuario: String, 
    val password: String, 
    val rol: String, 
    val activo: Boolean = true 
) 
