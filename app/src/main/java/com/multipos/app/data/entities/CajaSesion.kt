package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "caja_sesiones",
    indices = [Index("empresaId", "estado")]
)
data class CajaSesion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val abiertaPorUsuarioId: Int,
    val cerradaPorUsuarioId: Int? = null,
    val fechaApertura: Long,
    val fechaCierre: Long? = null,
    val montoApertura: Long,
    val montoEsperadoCierre: Long? = null,
    val montoContadoCierre: Long? = null,
    val diferenciaCierre: Long? = null,
    val estado: String,
    val notaCierre: String = ""
) {
    companion object {
        const val ESTADO_ABIERTA = "ABIERTA"
        const val ESTADO_CERRADA = "CERRADA"
    }
}
