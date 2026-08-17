package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.multipos.app.data.entities.Cliente
import com.multipos.app.data.entities.CredencialCliente

@Dao
interface CredencialClienteDao {
    @Insert
    suspend fun insert(credential: CredencialCliente): Long

    @Query("SELECT * FROM credenciales_clientes WHERE clienteId = :clientId AND empresaId = :companyId AND estado = 'ACTIVA' LIMIT 1")
    suspend fun getActiveForClient(clientId: Int, companyId: String): CredencialCliente?

    @Query("SELECT * FROM credenciales_clientes WHERE id = :credentialId AND empresaId = :companyId AND estado = 'ACTIVA' LIMIT 1")
    suspend fun getById(credentialId: Int, companyId: String): CredencialCliente?

    @Query("SELECT * FROM credenciales_clientes WHERE credentialId = :credentialId AND empresaId = :companyId LIMIT 1")
    suspend fun getForCredentialId(companyId: String, credentialId: String): CredencialCliente?

    @Query("SELECT c.* FROM clientes c INNER JOIN credenciales_clientes q ON q.clienteId = c.id WHERE q.credentialId = :credentialId AND q.empresaId = :companyId AND q.estado = 'ACTIVA' AND c.empresaId = :companyId AND c.activo = 1 LIMIT 1")
    suspend fun getAuthorizedClient(companyId: String, credentialId: String): Cliente?

    @Query("SELECT COUNT(*) FROM credenciales_clientes q INNER JOIN clientes c ON c.id = q.clienteId AND c.empresaId = q.empresaId WHERE q.credentialId = :credentialId AND q.clienteId = :clientId AND q.empresaId = :companyId AND q.estado = 'ACTIVA' AND c.activo = 1")
    suspend fun isActiveForClient(companyId: String, clientId: Int, credentialId: String): Int

    @Query("UPDATE credenciales_clientes SET estado = :newState, fechaRevocacion = :revokedAt WHERE clienteId = :clientId AND empresaId = :companyId AND estado = 'ACTIVA'")
    suspend fun revokeActive(clientId: Int, companyId: String, newState: String, revokedAt: Long): Int

    @Query("UPDATE credenciales_clientes SET intentosFallidos = intentosFallidos + 1 WHERE id = :credentialId AND empresaId = :companyId AND estado = 'ACTIVA'")
    suspend fun incrementPinAttempts(credentialId: Int, companyId: String): Int

    @Query("UPDATE credenciales_clientes SET bloqueadaHasta = :lockedUntil WHERE id = :credentialId AND empresaId = :companyId AND estado = 'ACTIVA'")
    suspend fun lockCredential(credentialId: Int, companyId: String, lockedUntil: Long): Int

    @Query("UPDATE credenciales_clientes SET intentosFallidos = 0, bloqueadaHasta = NULL WHERE id = :credentialId AND empresaId = :companyId AND estado = 'ACTIVA'")
    suspend fun resetPinAttempts(credentialId: Int, companyId: String): Int

    @Query("UPDATE credenciales_clientes SET ultimoUso = :now WHERE id = :credentialId AND empresaId = :companyId AND estado = 'ACTIVA'")
    suspend fun updateUltimoUso(credentialId: Int, companyId: String, now: Long): Int
}
