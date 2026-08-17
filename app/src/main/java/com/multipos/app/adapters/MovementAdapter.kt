package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.MovimientoCaja
import com.multipos.app.util.Money
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MovementAdapter : ListAdapter<MovimientoCaja, MovementAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_movement, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val txtTipo: TextView = view.findViewById(R.id.txtMovementType)
        private val txtConcepto: TextView = view.findViewById(R.id.txtMovementConcept)
        private val txtMonto: TextView = view.findViewById(R.id.txtMovementAmount)
        private val txtFecha: TextView = view.findViewById(R.id.txtMovementDate)

        fun bind(movimiento: MovimientoCaja) {
            val context = itemView.context
            txtTipo.text = movimiento.tipo
            txtConcepto.text = movimiento.concepto
            txtMonto.text = Money.format(movimiento.monto)
            val colorRes = if (movimiento.tipo.startsWith("INGRESO") || movimiento.tipo == MovimientoCaja.TIPO_APERTURA) {
                R.color.success
            } else {
                R.color.error
            }
            txtMonto.setTextColor(context.getColor(colorRes))
            txtFecha.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(movimiento.fecha))
        }
    }

    private object Diff : DiffUtil.ItemCallback<MovimientoCaja>() {
        override fun areItemsTheSame(a: MovimientoCaja, b: MovimientoCaja) = a.id == b.id
        override fun areContentsTheSame(a: MovimientoCaja, b: MovimientoCaja) = a == b
    }
}
