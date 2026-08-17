package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.Auditoria
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditoriaDao {
    @Insert
    suspend fun insert(auditoria: Auditoria): Long

    @Query("SELECT * FROM auditoria WHERE empresaId = :empresaId ORDER BY fecha DESC")
    fun getByCompany(empresaId: String): Flow<List<Auditoria>>

    @Query("SELECT * FROM auditoria WHERE empresaId = :empresaId AND fecha BETWEEN :start AND :end ORDER BY fecha DESC")
    suspend fun getByCompanyAndDateRange(empresaId: String, start: Long, end: Long): List<Auditoria>
}
