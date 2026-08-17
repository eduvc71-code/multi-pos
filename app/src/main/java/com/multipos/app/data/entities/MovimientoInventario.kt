package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "movimientos_inventario",
    foreignKeys = [
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Producto::class, parentColumns = ["id"], childColumns = ["productoId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Venta::class, parentColumns = ["id"], childColumns = ["ventaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Devolucion::class, parentColumns = ["id"], childColumns = ["devolucionId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("empresaId", "fecha"), Index("productoId"), Index("ventaId"), Index("devolucionId")]
)
data class MovimientoInventario(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val productoId: Int,
    val usuarioId: Int,
    val tipo: String,
    val cantidadFirmada: Int,
    val stockAnterior: Int,
    val stockPosterior: Int,
    val ventaId: Int? = null,
    val devolucionId: Long? = null,
    val motivo: String,
    val fecha: Long
) {
    companion object {
        const val TIPO_VENTA = "VENTA"
        const val TIPO_ANULACION = "ANULACION"
        const val TIPO_DEVOLUCION = "DEVOLUCION"
        const val TIPO_ENTRADA_MANUAL = "ENTRADA_MANUAL"
        const val TIPO_SALIDA_MANUAL = "SALIDA_MANUAL"
        const val TIPO_AJUSTE = "AJUSTE"
    }
}
