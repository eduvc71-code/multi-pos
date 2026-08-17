package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.multipos.app.data.entities.Cliente
import kotlinx.coroutines.flow.Flow

@Dao
interface ClienteDao {
    @Query("SELECT * FROM clientes WHERE empresaId = :empresaId AND activo = 1 ORDER BY nombre ASC") fun getAll(empresaId: String): Flow<List<Cliente>>
    @Query("SELECT * FROM clientes WHERE empresaId = :empresaId AND activo = 1 ORDER BY nombre ASC") suspend fun getAllOnce(empresaId: String): List<Cliente>
    @Insert suspend fun insert(cliente: Cliente): Long
    @Update suspend fun update(cliente: Cliente)
    @Query("UPDATE clientes SET activo = 0, creditoHabilitado = 0, estadoCredito = 'CANCELADO' WHERE id = :clientId AND empresaId = :companyId AND activo = 1 AND creditoActual = 0")
    suspend fun archive(clientId: Int, companyId: String): Int

    @Query("UPDATE clientes SET " +
            "nombre = :nombre, " +
            "documento = :documento, " +
            "telefono = :telefono, " +
            "limiteCredito = :limiteCredito, " +
            "creditoHabilitado = :creditoHabilitado, " +
            "estadoCredito = :estadoCredito, " +
            "fechaAprobacion = :fechaAprobacion, " +
            "usuarioAproboId = :usuarioAproboId " +
            "WHERE id = :id AND empresaId = :empresaId AND activo = 1 AND :limiteCredito >= creditoActual")
    suspend fun updateWithCreditCheck(
        id: Int,
        empresaId: String,
        nombre: String,
        documento: String,
        telefono: String,
        limiteCredito: Long,
        creditoHabilitado: Boolean,
        estadoCredito: String,
        fechaAprobacion: Long?,
        usuarioAproboId: Int?
    ): Int

    @Query("UPDATE clientes SET creditoActual = creditoActual + :amount WHERE id = :clientId AND empresaId = :empresaId AND activo = 1 AND creditoHabilitado = 1 AND estadoCredito = 'ACTIVO' AND creditoActual + :amount <= limiteCredito")
    suspend fun increaseCredit(clientId: Int, amount: Long, empresaId: String): Int
    @Query("UPDATE clientes SET creditoActual = creditoActual - :amount WHERE id = :clientId AND empresaId = :empresaId AND activo = 1 AND estadoCredito IN ('ACTIVO', 'SUSPENDIDO') AND creditoActual >= :amount")
    suspend fun decreaseCredit(clientId: Int, amount: Long, empresaId: String): Int

    @Query("SELECT * FROM clientes WHERE id = :clientId AND empresaId = :companyId AND activo = 1 LIMIT 1")
    suspend fun getById(clientId: Int, companyId: String): Cliente?

    @Query("SELECT * FROM clientes WHERE id = :clientId AND empresaId = :companyId LIMIT 1")
    suspend fun getByIdIncludingInactive(clientId: Int, companyId: String): Cliente?

    @Query("UPDATE clientes SET creditoHabilitado = :enabled, estadoCredito = :state, fechaAprobacion = :approvedAt, usuarioAproboId = :approvedBy, limiteCredito = :limit WHERE id = :clientId AND empresaId = :companyId AND activo = 1")
    suspend fun updateCreditAuthorization(clientId: Int, companyId: String, enabled: Boolean, state: String, approvedAt: Long?, approvedBy: Int?, limit: Long): Int
}
