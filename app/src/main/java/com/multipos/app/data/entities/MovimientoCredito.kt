package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos_credito",
    foreignKeys = [
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Cliente::class, parentColumns = ["id"], childColumns = ["clienteId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Venta::class, parentColumns = ["id"], childColumns = ["ventaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Abono::class, parentColumns = ["id"], childColumns = ["abonoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Devolucion::class, parentColumns = ["id"], childColumns = ["devolucionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("empresaId", "clienteId", "fecha"),
        Index("ventaId"),
        Index("abonoId"),
        Index("devolucionId")
    ]
)
data class MovimientoCredito(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val clienteId: Int,
    val usuarioId: Int,
    val tipo: String,
    val importeFirmado: Long,
    val saldoPosterior: Long,
    val ventaId: Int? = null,
    val abonoId: Long? = null,
    val devolucionId: Long? = null,
    val fecha: Long,
    val nota: String = ""
) {
    companion object {
        const val TIPO_VENTA_CREDITO = "VENTA_CREDITO"
        const val TIPO_ABONO = "ABONO"
        const val TIPO_DEVOLUCION = "DEVOLUCION"
        const val TIPO_ANULACION = "ANULACION"
    }
}