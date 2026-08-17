package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "abonos",
    foreignKeys = [
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Cliente::class, parentColumns = ["id"], childColumns = ["idCliente"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Venta::class, parentColumns = ["id"], childColumns = ["idVenta"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = CajaSesion::class, parentColumns = ["id"], childColumns = ["cajaSesionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("empresaId"),
        Index("empresaId", "idCliente"),
        Index("idVenta"),
        Index("cajaSesionId"),
        Index("usuarioId")
    ]
)
data class Abono(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val idCliente: Int,
    val idVenta: Int? = null,
    val cajaSesionId: Long? = null,
    val usuarioId: Int,
    val monto: Long,
    val medioPago: String,
    val fecha: Long = System.currentTimeMillis(),
    val nota: String = ""
) {
    companion object {
        const val MEDIO_EFECTIVO = "EFECTIVO"
        const val MEDIO_TARJETA = "TARJETA"
        const val MEDIO_TRANSFERENCIA = "TRANSFERENCIA"
    }
}
