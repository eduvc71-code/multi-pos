package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {
    @androidx.room.Query("SELECT * FROM ventas ORDER BY fecha DESC")
    fun getAll(): Flow<List<Venta>>

    @androidx.room.Query("SELECT COALESCE(SUM(total), 0) FROM ventas WHERE fecha >= :startOfDay")
    suspend fun totalSince(startOfDay: Long): Double

    @Insert
    suspend fun insert(venta: Venta): Long

    @Insert
    suspend fun insertDetalles(detalles: List<DetalleVenta>)
}
