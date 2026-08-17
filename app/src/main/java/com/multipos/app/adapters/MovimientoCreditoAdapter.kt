package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.util.Money

class MovimientoCreditoAdapter(
    private var rows: List<Row> = emptyList()
) : RecyclerView.Adapter<MovimientoCreditoAdapter.Holder>() {

    fun update(newRows: List<Row>) {
        rows = newRows
        notifyDataSetChanged()
    }

    data class Row(
        val fecha: String,
        val importe: Long,
        val tipoReferencia: String,
        val saldoPosterior: String,
        val usuario: String
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_movimiento_credito, parent, false))

    override fun getItemCount() = rows.size

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(rows[position])

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val fecha = view.findViewById<TextView>(R.id.txtMovimientoFecha)
        private val importe = view.findViewById<TextView>(R.id.txtMovimientoImporte)
        private val tipoReferencia = view.findViewById<TextView>(R.id.txtMovimientoTipoReferencia)
        private val saldoUsuario = view.findViewById<TextView>(R.id.txtMovimientoSaldoUsuario)

        fun bind(row: Row) {
            fecha.text = row.fecha
            importe.text = Money.format(row.importe)
            tipoReferencia.text = row.tipoReferencia
            saldoUsuario.text = "${row.saldoPosterior} · ${row.usuario}"
        }
    }
}