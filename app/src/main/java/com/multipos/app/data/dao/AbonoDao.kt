package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.Abono

@Dao
interface AbonoDao {
    @Insert suspend fun insert(abono: Abono): Long
    @Query("SELECT * FROM abonos WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Abono?
    @Query("SELECT COALESCE(SUM(monto), 0) FROM abonos WHERE idCliente = :clientId AND empresaId = :empresaId")
    suspend fun totalForClient(clientId: Int, empresaId: String): Long
    @Query("SELECT * FROM abonos WHERE empresaId = :empresaId AND idCliente = :clientId ORDER BY fecha DESC, id DESC")
    suspend fun getByClient(empresaId: String, clientId: Int): List<Abono>

    @Query("SELECT * FROM abonos WHERE empresaId = :empresaId AND fecha >= :desde AND fecha < :hastaExclusive ORDER BY fecha ASC, id ASC")
    suspend fun getInRange(empresaId: String, desde: Long, hastaExclusive: Long): List<Abono>
}