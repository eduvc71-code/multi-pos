package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos_caja",
    foreignKeys = [
        ForeignKey(entity = CajaSesion::class, parentColumns = ["id"], childColumns = ["cajaSesionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [
        Index("cajaSesionId", "fecha"),
        Index("empresaId", "fecha")
    ]
)
data class MovimientoCaja(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cajaSesionId: Long,
    val empresaId: String,
    val usuarioId: Int,
    val tipo: String,
    val monto: Long,
    val ventaId: Int? = null,
    val abonoId: Long? = null,
    val devolucionId: Long? = null,
    val concepto: String,
    val fecha: Long,
    val detalle: String = ""
) {
    companion object {
        const val TIPO_APERTURA = "APERTURA"
        const val TIPO_INGRESO_VENTA = "INGRESO_VENTA"
        const val TIPO_INGRESO_MANUAL = "INGRESO_MANUAL"
        const val TIPO_EGRESO_MANUAL = "EGRESO_MANUAL"
        const val TIPO_EGRESO_DEVOLUCION = "EGRESO_DEVOLUCION"
        const val TIPO_REVERSO_ANULACION = "REVERSO_ANULACION"
        const val TIPO_INGRESO_ABONO = "INGRESO_ABONO"
    }
}
