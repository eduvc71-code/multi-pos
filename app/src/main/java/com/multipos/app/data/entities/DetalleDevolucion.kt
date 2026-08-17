package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "detalle_devoluciones",
    foreignKeys = [
        ForeignKey(entity = Devolucion::class, parentColumns = ["id"], childColumns = ["devolucionId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = DetalleVenta::class, parentColumns = ["id"], childColumns = ["detalleVentaId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Producto::class, parentColumns = ["id"], childColumns = ["productoId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("devolucionId"), Index("detalleVentaId"), Index("productoId")]
)
data class DetalleDevolucion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val devolucionId: Long,
    val detalleVentaId: Int,
    val productoId: Int,
    val cantidad: Int,
    val precioUnitario: Long,
    val subtotal: Long
)
