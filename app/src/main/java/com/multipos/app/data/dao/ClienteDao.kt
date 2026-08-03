package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.multipos.app.data.entities.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes ORDER BY nombre ASC") fun getAll(): Flow<List<Cliente>>
    @Insert suspend fun insert(cliente: Cliente): Long
    @Update suspend fun update(cliente: Cliente)
    @Delete suspend fun delete(cliente: Cliente)
}
