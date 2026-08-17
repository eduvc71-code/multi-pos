package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "devoluciones",
    foreignKeys = [
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Venta::class, parentColumns = ["id"], childColumns = ["ventaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CajaSesion::class, parentColumns = ["id"], childColumns = ["cajaSesionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("empresaId", "fecha"), Index("ventaId"), Index("cajaSesionId")]
)
data class Devolucion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val ventaId: Int,
    val usuarioId: Int,
    val cajaSesionId: Long? = null,
    val monto: Long,
    val medioReembolso: String,
    val estadoReembolso: String,
    val motivo: String,
    val fecha: Long
) {
    companion object {
        const val ESTADO_COMPLETADO = "COMPLETADO"
        const val ESTADO_CONFIRMADO_EXTERNAMENTE = "CONFIRMADO_EXTERNAMENTE"
    }
}
