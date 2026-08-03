package com.multipos.app.data.dao  
  
import androidx.room.*  
import com.multipos.app.data.entities.Usuario  
  
@Dao  
interface UsuarioDao {  
    @Query("SELECT * FROM usuarios WHERE usuario = :user AND password = :pass LIMIT 1")  
    suspend fun login(user: String, pass: String): Usuario?  
  
    @Insert(onConflict = OnConflictStrategy.REPLACE)  
    suspend fun insert(usuario: Usuario)  

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int
}
