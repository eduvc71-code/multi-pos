package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.multipos.app.data.entities.Empresa
import kotlinx.coroutines.flow.Flow

@Dao
interface EmpresaDao {
    @Query("SELECT * FROM empresas WHERE activa = 1 ORDER BY nombre ASC")
    fun getAll(): Flow<List<Empresa>>

    @Query("SELECT * FROM empresas WHERE activa = 1 ORDER BY nombre ASC")
    suspend fun getAllOnce(): List<Empresa>

    @Query("SELECT * FROM empresas WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Empresa?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(empresa: Empresa)

    @Query("SELECT COUNT(*) FROM empresas")
    suspend fun count(): Int

    @Query("DELETE FROM empresas WHERE id = :id")
    suspend fun deleteById(id: String)
}
