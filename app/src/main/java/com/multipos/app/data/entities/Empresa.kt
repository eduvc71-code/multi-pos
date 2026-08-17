package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "empresas")
data class Empresa(
    @PrimaryKey val id: String,
    val nombre: String,
    val tipoNegocio: String = TIPO_TIENDA,
    val colorPrimarioHex: String = "#2563EB",
    val logoUri: String? = null,
    val activa: Boolean = true,
    val nit: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val fechaCreacion: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_ID = "default-company"
        const val TIPO_TIENDA = "TIENDA"
        const val TIPO_FERRETERIA = "FERRETERIA"
        const val TIPO_RESTAURANTE = "RESTAURANTE"
        const val TIPO_FARMACIA = "FARMACIA"
    }
}
