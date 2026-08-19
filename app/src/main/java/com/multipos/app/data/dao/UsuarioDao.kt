package com.multipos.app.data.dao  
  
import androidx.room.*  
import com.multipos.app.data.entities.Usuario  
  
@Dao  
interface UsuarioDao {  
    @Query("SELECT * FROM usuarios WHERE usuario = :user AND activo = 1 LIMIT 1")
    suspend fun getByUsername(user: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE id = :id AND activo = 1 LIMIT 1")
    suspend fun getById(id: Int): Usuario?

    @Query("""
        SELECT u.id, u.nombre, u.usuario, u.password, u.passwordHash, u.passwordSalt,
               ue.rol AS rol, u.empresaId,
               CASE WHEN u.activo = 1 AND ue.activo = 1 THEN 1 ELSE 0 END AS activo,
               u.requiereCambioClave, u.fechaCreacion,
               u.intentosFallidos, u.bloqueadoHasta, u.ultimoLogin
        FROM usuarios u
        INNER JOIN usuario_empresas ue ON ue.usuarioId = u.id
        WHERE ue.empresaId = :empresaId
        ORDER BY u.nombre
    """)
    fun getByCompany(empresaId: String): kotlinx.coroutines.flow.Flow<List<Usuario>>
  
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(usuario: Usuario): Long

    @Update
    suspend fun update(usuario: Usuario)

    @Query("UPDATE usuarios SET nombre = :nombre WHERE id = :id")
    suspend fun updateName(id: Int, nombre: String)

    @Query("SELECT COUNT(*) FROM usuarios")
    suspend fun count(): Int
@Query("UPDATE usuarios SET intentosFallidos = :attempts, bloqueadoHasta = :lockedUntil WHERE id = :id")
    suspend fun setLoginBlock(id: Int, attempts: Int, lockedUntil: Long?)

    @Query("UPDATE usuarios SET intentosFallidos = 0, bloqueadoHasta = NULL, ultimoLogin = :now WHERE id = :id")
    suspend fun resetLoginStateAndTouch(id: Int, now: Long)

    @Query("UPDATE usuarios SET ultimoLogin = :now WHERE id = :id")
    suspend fun touchLastLogin(id: Int, now: Long)

    @Query("UPDATE usuarios SET intentosFallidos = intentosFallidos + 1 WHERE id = :id")
    suspend fun incrementLoginFailures(id: Int)

    @Query("UPDATE usuarios SET password = '', passwordHash = :hash, passwordSalt = :salt, requiereCambioClave = 1 WHERE id = :id")
    suspend fun upgradeLegacyPassword(id: Int, hash: String, salt: String)

    @Query("DELETE FROM usuarios WHERE empresaId = :empresaId")
    suspend fun deleteByCompany(empresaId: String)
}
