package com.multipos.app.data

import androidx.room.withTransaction
import com.multipos.app.data.entities.Auditoria
import com.multipos.app.data.entities.Usuario
import com.multipos.app.security.PasswordHasher

class AuthRepository(private val database: AppDatabase) {
    suspend fun authenticate(username: String, password: CharArray): Usuario? {
        val normalized = username.trim()
        val db = database
        return try {
            db.withTransaction {
                val ora = System.currentTimeMillis()
                val existing = db.usuarioDao().getByUsername(normalized)

                // Usuario o contraseña incorrecta comparten el mismo resultado genérico (null).
                if (existing == null) return@withTransaction null

                // Bloqueo vigente: rechazo inmediato, sin verificar contraseña.
                val blockedUntil = existing.bloqueadoHasta
                if (blockedUntil != null && ora < blockedUntil) {
                    db.auditoriaDao().insert(
                        Auditoria(
                            empresaId = existing.empresaId,
                            usuarioId = existing.id,
                            accion = Auditoria.ACCION_LOGIN_BLOQUEADO,
                            entidad = "usuario",
                            entidadId = existing.id.toString(),
                            detalle = "intento de login en periodo de bloqueo",
                            fecha = ora
                        )
                    )
                    return@withTransaction null
                }

                // Bloqueo vencido: se limpia antes de seguir evaluando.
                if (blockedUntil != null) {
                    db.usuarioDao().setLoginBlock(existing.id, existing.intentosFallidos, null)
                }

                val isValid = verifyPassword(existing, password)
                if (!isValid) {
                    db.usuarioDao().incrementLoginFailures(existing.id)
                    val updated = db.usuarioDao().getById(existing.id)
                    if (updated != null && updated.intentosFallidos >= Usuario.MAX_LOGIN_ATTEMPTS) {
                        val lockUntil = ora + Usuario.LOGIN_LOCKOUT_DURATION_MS
                        db.usuarioDao().setLoginBlock(updated.id, updated.intentosFallidos, lockUntil)
                        db.auditoriaDao().insert(
                            Auditoria(
                                empresaId = updated.empresaId,
                                usuarioId = updated.id,
                                accion = Auditoria.ACCION_LOGIN_BLOQUEADO,
                                entidad = "usuario",
                                entidadId = updated.id.toString(),
                                detalle = "usuario bloqueado por intentos fallidos",
                                fecha = ora
                            )
                        )
                    }
                    return@withTransaction null
                }

                // Login correcto: resetea bloqueo, reinicia intentos y registra último login.
                db.usuarioDao().resetLoginStateAndTouch(existing.id, ora)
                if (existing.passwordHash == null || existing.passwordSalt == null) {
                    upgradeLegacyPassword(existing)
                }
                db.auditoriaDao().insert(
                    Auditoria(
                        empresaId = existing.empresaId,
                        usuarioId = existing.id,
                        accion = Auditoria.ACCION_LOGIN_OK,
                        entidad = "usuario",
                        entidadId = existing.id.toString(),
                        detalle = "login correcto",
                        fecha = ora
                    )
                )
                db.usuarioDao().getById(existing.id)
            }
        } finally {
            // Nunca conservar la contraseña en memoria, aunque la verificación lance excepción.
            password.fill('\u0000')
        }
    }

    private fun verifyPassword(user: Usuario, password: CharArray): Boolean = when {
        user.passwordHash != null && user.passwordSalt != null ->
            PasswordHasher.verify(password, user.passwordHash, user.passwordSalt)
        user.password.isNotEmpty() -> user.password.toCharArray().contentEquals(password)
        else -> false
    }

    private suspend fun upgradeLegacyPassword(user: Usuario) {
        // Usuario legacy (contraseña en texto plano): se hashea y obliga a cambiar clave.
        if (user.passwordHash != null && user.passwordSalt != null) return
        if (user.password.isBlank()) return
        val digest = PasswordHasher.hash(user.password.toCharArray())
        database.usuarioDao().upgradeLegacyPassword(user.id, digest.hash, digest.salt)
    }
}
