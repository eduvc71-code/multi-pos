package com.multipos.app.data.dao  
  
import androidx.room.*  
import com.multipos.app.data.entities.Producto  
import kotlinx.coroutines.flow.Flow  
  
@Dao  
interface ProductoDao {  
    @Query("SELECT * FROM productos ORDER BY nombre ASC")  
    fun getAll(): Flow<List<Producto>>  
  
    @Insert(onConflict = OnConflictStrategy.REPLACE)  
    suspend fun insert(product: Producto): Long
  
    @Update  
    suspend fun update(product: Producto)  

    @Delete
    suspend fun delete(product: Producto)

    @Query("UPDATE productos SET stock = stock - :quantity WHERE id = :productId AND stock >= :quantity")
    suspend fun decreaseStock(productId: Int, quantity: Int): Int

    @Query("SELECT COUNT(*) FROM productos")
    suspend fun count(): Int
}
