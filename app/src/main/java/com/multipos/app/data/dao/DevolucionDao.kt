package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.DetalleDevolucion
import com.multipos.app.data.entities.Devolucion

@Dao
interface DevolucionDao {
    @Insert
    suspend fun insert(refund: Devolucion): Long

    @Insert
    suspend fun insertDetails(details: List<DetalleDevolucion>)

    @Query("SELECT * FROM devoluciones WHERE empresaId = :companyId AND ventaId = :saleId ORDER BY fecha ASC")
    suspend fun getBySale(companyId: String, saleId: Int): List<Devolucion>

    @Query("SELECT * FROM devoluciones WHERE empresaId = :companyId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC")
    suspend fun getInRange(companyId: String, desde: Long, hastaExclusive: Long): List<Devolucion>

    @Query("SELECT * FROM detalle_devoluciones WHERE devolucionId = :refundId ORDER BY id ASC")
    suspend fun getDetails(refundId: Long): List<DetalleDevolucion>

    @Query("SELECT COALESCE(SUM(dd.cantidad), 0) FROM detalle_devoluciones dd INNER JOIN devoluciones d ON d.id = dd.devolucionId WHERE d.empresaId = :companyId AND d.ventaId = :saleId AND dd.detalleVentaId = :saleDetailId")
    suspend fun returnedQuantity(companyId: String, saleId: Int, saleDetailId: Int): Int
}
