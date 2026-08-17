package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.MovimientoCredito

@Dao
interface MovimientoCreditoDao {
    @Insert suspend fun insert(movimiento: MovimientoCredito): Long

    @Query("SELECT * FROM movimientos_credito WHERE empresaId = :empresaId AND clienteId = :clientId ORDER BY fecha DESC, id DESC")
    suspend fun getByClient(empresaId: String, clientId: Int): List<MovimientoCredito>

    @Query("SELECT * FROM movimientos_credito WHERE empresaId = :empresaId AND clienteId = :clientId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha DESC, id DESC")
    suspend fun getByClientBetween(empresaId: String, clientId: Int, desde: Long, hastaExclusive: Long): List<MovimientoCredito>

    @Query("SELECT COALESCE(SUM(importeFirmado), 0) FROM movimientos_credito WHERE empresaId = :empresaId AND clienteId = :clientId")
    suspend fun sumImporteFirmado(empresaId: String, clientId: Int): Long

    @Query("SELECT * FROM movimientos_credito WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MovimientoCredito?

    @Query("SELECT * FROM movimientos_credito WHERE empresaId = :empresaId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC, id ASC")
    suspend fun getByCompanyBetween(empresaId: String, desde: Long, hastaExclusive: Long): List<MovimientoCredito>
}