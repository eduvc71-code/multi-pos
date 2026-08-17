package com.multipos.app.data.dao

import androidx.room.*
import com.multipos.app.data.entities.Producto
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductoDao {
    @Query("SELECT * FROM productos WHERE empresaId = :empresaId AND activo = 1 ORDER BY nombre ASC")
    fun getAll(empresaId: String): Flow<List<Producto>>

    @Query("SELECT * FROM productos WHERE empresaId = :empresaId ORDER BY nombre ASC")
    suspend fun getAllOnce(empresaId: String): List<Producto>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(product: Producto): Long

    @Update
    suspend fun update(product: Producto)

    @Query("UPDATE productos SET activo = 0 WHERE id = :productId AND empresaId = :empresaId AND activo = 1")
    suspend fun archive(productId: Int, empresaId: String): Int

    @Query("SELECT * FROM productos WHERE id = :productId AND empresaId = :empresaId LIMIT 1")
    suspend fun getByIdIncludingInactive(productId: Int, empresaId: String): Producto?

    @Query("UPDATE productos SET stock = stock - :quantity WHERE id = :productId AND empresaId = :empresaId AND activo = 1 AND stock >= :quantity")
    suspend fun decreaseStock(productId: Int, quantity: Int, empresaId: String): Int

    @Query("UPDATE productos SET stock = stock + :quantity WHERE id = :productId AND empresaId = :empresaId AND stock <= 2147483647 - :quantity")
    suspend fun increaseStock(productId: Int, quantity: Int, empresaId: String): Int

    @Query("UPDATE productos SET stock = :newStock WHERE id = :productId AND empresaId = :empresaId AND activo = 1 AND :newStock >= 0")
    suspend fun setStock(productId: Int, newStock: Int, empresaId: String): Int

    @Query("SELECT COUNT(*) FROM productos WHERE empresaId = :empresaId AND activo = 1")
    suspend fun count(empresaId: String): Int

    @Query("SELECT COUNT(*) FROM productos WHERE empresaId = :empresaId AND activo = 1 AND stock <= stockMinimo")
    suspend fun lowStockCount(empresaId: String): Int

    @Query("SELECT * FROM productos WHERE empresaId = :empresaId AND activo = 1 AND (codigo = :code OR codigoBarras = :code) AND stock >= 0 LIMIT 1")
    suspend fun getByCode(empresaId: String, code: String): Producto?
}
