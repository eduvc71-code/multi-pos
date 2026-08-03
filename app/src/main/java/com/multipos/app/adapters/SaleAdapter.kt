package com.multipos.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.multipos.app.R
import com.multipos.app.data.entities.Venta
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SaleAdapter : ListAdapter<Venta, SaleAdapter.Holder>(Diff) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false))
    override fun onBindViewHolder(holder: Holder, position: Int) = holder.bind(getItem(position))
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.txtSaleTitle)
        private val detail = view.findViewById<TextView>(R.id.txtSaleDetail)
        fun bind(sale: Venta) {
            title.text = "Venta #${sale.id} · ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(sale.total)}"
            detail.text = "${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(sale.fecha))} · ${sale.tipoPago} · ${sale.estado}"
        }
    }
    private object Diff : DiffUtil.ItemCallback<Venta>() {
        override fun areItemsTheSame(a: Venta, b: Venta) = a.id == b.id
        override fun areContentsTheSame(a: Venta, b: Venta) = a == b
    }
}
