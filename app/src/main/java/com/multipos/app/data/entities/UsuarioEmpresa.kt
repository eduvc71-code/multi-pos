package com.multipos.app.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "usuario_empresas",
    primaryKeys = ["usuarioId", "empresaId"],
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["usuarioId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Empresa::class, parentColumns = ["id"], childColumns = ["empresaId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("usuarioId"), Index("empresaId")]
)
data class UsuarioEmpresa(
    val usuarioId: Int,
    val empresaId: String,
    val rol: String,
    val activo: Boolean = true,
    val fechaCreacion: Long = System.currentTimeMillis()
)
