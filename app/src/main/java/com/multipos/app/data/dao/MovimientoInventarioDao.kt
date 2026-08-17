package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.MovimientoInventario

@Dao
interface MovimientoInventarioDao {
    @Insert
    suspend fun insert(movement: MovimientoInventario): Long

    @Query("SELECT * FROM movimientos_inventario WHERE empresaId = :companyId AND productoId = :productId ORDER BY fecha DESC")
    suspend fun getByProduct(companyId: String, productId: Int): List<MovimientoInventario>

    @Query("SELECT * FROM movimientos_inventario WHERE empresaId = :companyId AND fecha BETWEEN :start AND :end ORDER BY fecha DESC")
    suspend fun getByDateRange(companyId: String, start: Long, end: Long): List<MovimientoInventario>
}
