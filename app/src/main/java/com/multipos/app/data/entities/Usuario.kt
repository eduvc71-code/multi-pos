package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.Index
import androidx.room.PrimaryKey 
 
@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true), Index(value = ["empresaId"])]
)
data class Usuario( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val nombre: String, 
    val usuario: String, 
    /** Solo existe para migrar instalaciones antiguas; los usuarios nuevos lo mantienen vacío. */
    val password: String = "",
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val rol: String,
    val empresaId: String = Empresa.DEFAULT_ID,
    val activo: Boolean = true,
    val requiereCambioClave: Boolean = false,
    val fechaCreacion: Long = System.currentTimeMillis(),
    /** Intentos fallidos consecutivos desde el último login correcto. */
    val intentosFallidos: Int = 0,
    /** Epoch ms hasta el que el usuario queda bloqueado. Nulo si no está bloqueado. */
    val bloqueadoHasta: Long? = null,
    /** Epoch ms del último login correcto. */
    val ultimoLogin: Long? = null
) {
    companion object {
        const val ROL_PROPIETARIO = "PROPIETARIO"
        const val ROL_ADMINISTRADOR = "ADMINISTRADOR"
        const val ROL_CAJERO = "CAJERO"
        const val ROL_VENDEDOR = "VENDEDOR"
        const val MAX_LOGIN_ATTEMPTS = 5
        const val LOGIN_LOCKOUT_DURATION_MS = 15L * 60L * 1000L
    }
}
