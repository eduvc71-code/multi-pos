package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "auditoria",
    indices = [
        Index("empresaId", "fecha"),
        Index("empresaId", "accion"),
        Index("usuarioId")
    ]
)
data class Auditoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val empresaId: String,
    val usuarioId: Int? = null,
    val accion: String,
    val entidad: String,
    val entidadId: String? = null,
    val detalle: String,
    val fecha: Long = System.currentTimeMillis()
) {
    companion object {
        const val ACCION_LOGIN_OK = "LOGIN_OK"
        const val ACCION_LOGIN_BLOQUEADO = "LOGIN_BLOQUEADO"
        const val ACCION_CAJA_APERTURA = "CAJA_APERTURA"
        const val ACCION_CAJA_CIERRE = "CAJA_CIERRE"
        const val ACCION_INGRESO_MANUAL = "INGRESO_MANUAL"
        const val ACCION_EGRESO_MANUAL = "EGRESO_MANUAL"
        const val ACCION_VENTA = "VENTA"
        const val ACCION_CREDENCIAL_EMISION = "CREDENCIAL_EMISION"
        const val ACCION_CREDENCIAL_REEMPLAZO = "CREDENCIAL_REEMPLAZO"
        const val ACCION_CREDENCIAL_REVOCACION = "CREDENCIAL_REVOCACION"
        const val ACCION_CREDITO_CAMBIO_ESTADO = "CREDITO_CAMBIO_ESTADO"
        const val ACCION_ABONO = "ABONO"
        const val ACCION_ANULACION = "ANULACION"
        const val ACCION_DEVOLUCION = "DEVOLUCION"
        const val ACCION_MOVIMIENTO_INVENTARIO = "MOVIMIENTO_INVENTARIO"
        const val ACCION_USUARIO_ALTA = "USUARIO_ALTA"
        const val ACCION_USUARIO_CAMBIO_ESTADO = "USUARIO_CAMBIO_ESTADO"
        const val ACCION_USUARIO_CAMBIO_ROL = "USUARIO_CAMBIO_ROL"
        const val ACCION_CONFIGURACION = "CONFIGURACION"
    }
}
