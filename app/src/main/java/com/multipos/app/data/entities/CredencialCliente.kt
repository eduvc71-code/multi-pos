package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credenciales_clientes",
    foreignKeys = [
        ForeignKey(entity = Cliente::class, parentColumns = ["id"], childColumns = ["clienteId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("clienteId"), Index("empresaId"), Index(value = ["credentialId"], unique = true), Index("empresaId", "estado")]
)
data class CredencialCliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clienteId: Int,
    val empresaId: String,
    val credentialId: String,
    val version: Int = 1,
    val estado: String = ESTADO_ACTIVA,
    val fechaEmision: Long = System.currentTimeMillis(),
    val fechaRevocacion: Long? = null,
    val emitidaPorUsuarioId: Int,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val fechaVencimiento: Long? = null,
    val intentosFallidos: Int = 0,
    val bloqueadaHasta: Long? = null,
    val ultimoUso: Long? = null
) {
    companion object {
        const val ESTADO_ACTIVA = "ACTIVA"
        const val ESTADO_REVOCADA = "REVOCADA"
        const val ESTADO_REEMPLAZADA = "REEMPLAZADA"
        const val PIN_LENGTH = 4
        const val DURATION_LOCKOUT_MS: Long = 15 * 60 * 1000L
        const val DURATION_CREDENTIAL_VALID_MS: Long = 365 * 24 * 60 * 60 * 1000L
        const val MAX_PIN_ATTEMPTS = 5
    }
}
