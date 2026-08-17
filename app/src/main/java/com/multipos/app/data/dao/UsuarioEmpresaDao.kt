package com.multipos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.multipos.app.data.entities.Empresa
import com.multipos.app.data.entities.Usuario
import com.multipos.app.data.entities.UsuarioEmpresa
import kotlinx.coroutines.flow.Flow

@Dao
interface UsuarioEmpresaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(membership: UsuarioEmpresa)

    @Query("SELECT e.* FROM empresas e INNER JOIN usuario_empresas ue ON ue.empresaId = e.id WHERE ue.usuarioId = :userId AND ue.activo = 1 AND e.activa = 1 ORDER BY e.nombre")
    fun getCompaniesForUser(userId: Int): Flow<List<Empresa>>

    @Query("SELECT * FROM usuario_empresas WHERE usuarioId = :userId AND empresaId = :companyId AND activo = 1 LIMIT 1")
    suspend fun getActiveMembership(userId: Int, companyId: String): UsuarioEmpresa?

    @Transaction
    suspend fun setActive(userId: Int, companyId: String, active: Boolean): Int {
        val updated = updateMembershipActive(userId, companyId, active)
        if (updated > 0 && active) reactivateUserAccount(userId)
        return updated
    }

    @Query("UPDATE usuario_empresas SET activo = :active WHERE usuarioId = :userId AND empresaId = :companyId")
    suspend fun updateMembershipActive(userId: Int, companyId: String, active: Boolean): Int

    @Query("UPDATE usuarios SET activo = 1 WHERE id = :userId")
    suspend fun reactivateUserAccount(userId: Int): Int

    @Query("SELECT u.* FROM usuarios u INNER JOIN usuario_empresas ue ON ue.usuarioId = u.id WHERE ue.empresaId = :companyId AND ue.activo = 1")
    fun getUsersForCompany(companyId: String): Flow<List<Usuario>>
}
