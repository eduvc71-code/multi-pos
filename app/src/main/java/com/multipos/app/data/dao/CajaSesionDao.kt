package com.multipos.app.data.dao

import androidx.room.*
import com.multipos.app.data.entities.CajaSesion
import kotlinx.coroutines.flow.Flow

@Dao
interface CajaSesionDao {
    @Insert
    suspend fun insert(cajaSesion: CajaSesion): Long

    @Update
    suspend fun update(cajaSesion: CajaSesion)

    @Query("SELECT * FROM caja_sesiones WHERE empresaId = :empresaId AND estado = 'ABIERTA' LIMIT 1")
    suspend fun getActiveSession(empresaId: String): CajaSesion?

    @Query("SELECT * FROM caja_sesiones WHERE empresaId = :empresaId AND estado = 'ABIERTA' ORDER BY fechaApertura DESC LIMIT 1")
    suspend fun getActiveSessionForCompany(empresaId: String): CajaSesion?

    @Query("SELECT * FROM caja_sesiones WHERE empresaId = :empresaId ORDER BY fechaApertura DESC")
    fun getAllSessions(empresaId: String): Flow<List<CajaSesion>>

    @Query("SELECT * FROM caja_sesiones WHERE id = :id AND empresaId = :empresaId LIMIT 1")
    suspend fun getById(id: Long, empresaId: String): CajaSesion?

    @Query("UPDATE caja_sesiones SET estado = 'CERRADA', cerradaPorUsuarioId = :cerradaPor, fechaCierre = :fechaCierre, montoEsperadoCierre = :montoEsperado, montoContadoCierre = :montoContado, diferenciaCierre = :diferencia, notaCierre = :nota WHERE id = :id AND empresaId = :empresaId AND estado = 'ABIERTA'")
    suspend fun cerrarSesion(id: Long, empresaId: String, cerradaPor: Int, fechaCierre: Long, montoEsperado: Long, montoContado: Long, diferencia: Long, nota: String): Int
}
