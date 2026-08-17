package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.DetalleVenta
import com.multipos.app.data.entities.Venta
import kotlinx.coroutines.flow.Flow

@Dao
interface VentaDao {
    @Query("SELECT * FROM ventas WHERE empresaId = :empresaId ORDER BY fecha DESC")
    fun getAll(empresaId: String): Flow<List<Venta>>

    @Query("SELECT COALESCE(SUM(total), 0) FROM ventas WHERE fecha >= :startOfDay AND empresaId = :empresaId")
    suspend fun totalSince(startOfDay: Long, empresaId: String): Long

    @Query("SELECT * FROM ventas WHERE id = :ventaId AND empresaId = :empresaId LIMIT 1")
    suspend fun getById(ventaId: Int, empresaId: String): Venta?

    @Insert
    suspend fun insert(venta: Venta): Long

    @Insert
    suspend fun insertDetalles(detalles: List<DetalleVenta>): List<Long>

    @Query("SELECT * FROM detalle_ventas WHERE idVenta = :saleId AND empresaId = :companyId ORDER BY id ASC")
    suspend fun getDetails(saleId: Int, companyId: String): List<DetalleVenta>

    @Query("SELECT * FROM detalle_ventas WHERE id = :id LIMIT 1")
    suspend fun getDetalleById(id: Int): DetalleVenta?

    @Query("UPDATE ventas SET cajaSesionId = :cajaSesionId WHERE id = :ventaId AND empresaId = :empresaId")
    suspend fun updateCajaSesion(ventaId: Int, empresaId: String, cajaSesionId: Long): Int

    @Query("UPDATE ventas SET estado = 'ANULADA', anuladaPorUsuarioId = :usuarioId, fechaAnulacion = :fecha, motivoAnulacion = :motivo WHERE id = :ventaId AND empresaId = :empresaId AND estado = 'COMPLETADA'")
    suspend fun anularVenta(ventaId: Int, empresaId: String, usuarioId: Int, fecha: Long, motivo: String): Int

    @Query("SELECT COALESCE(SUM(total), 0) FROM ventas WHERE empresaId = :empresaId AND estado = 'COMPLETADA' AND tipoPago = 'EFECTIVO' AND cajaSesionId = :cajaSesionId")
    suspend fun totalEfectivoPorCaja(empresaId: String, cajaSesionId: Long): Long

    @Query("SELECT * FROM ventas WHERE empresaId = :empresaId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC, id ASC")
    suspend fun getInRange(empresaId: String, desde: Long, hastaExclusive: Long): List<Venta>

    @Query("SELECT * FROM ventas WHERE empresaId = :empresaId AND estado = :estado AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC, id ASC")
    suspend fun getInRangeByEstado(empresaId: String, estado: String, desde: Long, hastaExclusive: Long): List<Venta>
}
