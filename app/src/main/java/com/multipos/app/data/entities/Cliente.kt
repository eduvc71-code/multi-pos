package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "clientes",
    indices = [
        Index(value = ["empresaId"]),
        Index(value = ["empresaId", "documento"], unique = true)
    ]
)
data class Cliente(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nombre: String,
    val documento: String,
    val telefono: String = "",
    val direccion: String = "",
    val limiteCredito: Long = 0,
    val creditoActual: Long = 0,
    val creditoHabilitado: Boolean = false,
    val estadoCredito: String = ESTADO_NO_SOLICITADO,
    val fechaInscripcion: Long = System.currentTimeMillis(),
    val fechaAprobacion: Long? = null,
    val usuarioAproboId: Int? = null,
    val activo: Boolean = true,
    val empresaId: String = Empresa.DEFAULT_ID
) {
    companion object {
        const val ESTADO_NO_SOLICITADO = "NO_SOLICITADO"
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_ACTIVO = "ACTIVO"
        const val ESTADO_SUSPENDIDO = "SUSPENDIDO"
        const val ESTADO_CANCELADO = "CANCELADO"
    }

    val creditoDisponible: Long get() = (limiteCredito - creditoActual).coerceAtLeast(0)
    val deuda: Long get() = creditoActual
}