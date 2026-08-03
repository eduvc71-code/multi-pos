package com.multipos.app.data.dao  
  
import androidx.room.*  
import com.multipos.app.data.entities.Producto  
import kotlinx.coroutines.flow.Flow  
  
@Dao  
interface ProductoDao {  
    @Query("SELECT * FROM productos ORDER BY nombre ASC")  
    fun getAll(): Flow<List<Producto>>  
  
    @Insert(onConflict = OnConflictStrategy.REPLACE)  
    suspend fun insert(product: Producto)  
  
    @Update  
    suspend fun update(product: Producto)  
} 
