package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.MovimientoCaja
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoCajaDao {
    @Insert
    suspend fun insert(movimiento: MovimientoCaja): Long

    @Query("SELECT * FROM movimientos_caja WHERE cajaSesionId = :cajaSesionId AND empresaId = :empresaId ORDER BY fecha ASC")
    suspend fun getBySession(cajaSesionId: Long, empresaId: String): List<MovimientoCaja>

    @Query("SELECT * FROM movimientos_caja WHERE empresaId = :empresaId AND fecha BETWEEN :start AND :end ORDER BY fecha ASC")
    suspend fun getByCompanyAndDateRange(empresaId: String, start: Long, end: Long): List<MovimientoCaja>

    @Query("SELECT * FROM movimientos_caja WHERE empresaId = :empresaId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC")
    suspend fun getByCompanyBetween(empresaId: String, desde: Long, hastaExclusive: Long): List<MovimientoCaja>

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE cajaSesionId = :cajaSesionId AND empresaId = :empresaId AND tipo IN ('INGRESO_VENTA', 'INGRESO_MANUAL', 'INGRESO_ABONO')")
    suspend fun totalIngresos(cajaSesionId: Long, empresaId: String): Long

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE cajaSesionId = :cajaSesionId AND empresaId = :empresaId AND tipo IN ('EGRESO_MANUAL', 'EGRESO_DEVOLUCION', 'REVERSO_ANULACION')")
    suspend fun totalEgresos(cajaSesionId: Long, empresaId: String): Long

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE cajaSesionId = :cajaSesionId AND empresaId = :empresaId AND tipo IN ('INGRESO_VENTA', 'INGRESO_MANUAL', 'INGRESO_ABONO') AND fecha >= :desde AND fecha < :hastaExclusive")
    suspend fun totalIngresosEnRango(cajaSesionId: Long, empresaId: String, desde: Long, hastaExclusive: Long): Long

    @Query("SELECT COALESCE(SUM(monto), 0) FROM movimientos_caja WHERE cajaSesionId = :cajaSesionId AND empresaId = :empresaId AND tipo IN ('EGRESO_MANUAL', 'EGRESO_DEVOLUCION', 'REVERSO_ANULACION') AND fecha >= :desde AND fecha < :hastaExclusive")
    suspend fun totalEgresosEnRango(cajaSesionId: Long, empresaId: String, desde: Long, hastaExclusive: Long): Long
}
