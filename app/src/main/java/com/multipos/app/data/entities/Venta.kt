package com.multipos.app.data.entities 
 
import androidx.room.Entity 
import androidx.room.PrimaryKey 
import androidx.room.Index
 
@Entity(tableName = "ventas", indices = [Index(value = ["empresaId"]), Index("cajaSesionId")])
data class Venta( 
    @PrimaryKey(autoGenerate = true) val id: Int = 0, 
    val fecha: Long = System.currentTimeMillis(), 
    val tipoPago: String, 
    val total: Long,
    val subtotal: Long = total,
    val descuento: Long = 0,
    val impuesto: Long = 0,
    val idCliente: Int? = null, 
    val idUsuario: Int, 
    val estado: String = "COMPLETADA",
    val empresaId: String = Empresa.DEFAULT_ID,
    val cajaSesionId: Long? = null,
    val anuladaPorUsuarioId: Int? = null,
    val fechaAnulacion: Long? = null,
    val motivoAnulacion: String? = null
) {
    companion object {
        const val ESTADO_COMPLETADA = "COMPLETADA"
        const val ESTADO_ANULADA = "ANULADA"
    }
}
